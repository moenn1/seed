package com.seed.bytecode;

import com.seed.ast.Expr;
import com.seed.ast.Stmt;
import com.seed.lexer.Token;

import java.util.*;

import static com.seed.bytecode.Opcode.*;

public final class Compiler {
    private final Module mod = new Module();

    private static final class FuncCtx {
        final Function fn;
        final Map<String, Integer> locals = new HashMap<>();
        int nextLocal = 0;
        FuncCtx(Function fn) { this.fn = fn; }
    }

    private final Deque<FuncCtx> stack = new ArrayDeque<>();

    public Module compile(List<Stmt> program) {
        // Predeclare functions (name and arity) to allow direct calls
        List<Stmt.Fun> funs = new ArrayList<>();
        for (Stmt s : program) if (s instanceof Stmt.Fun f) funs.add(f);
        // Add 'main' as synthetic top-level function (arity 0)
        Function main = new Function("main", 0, 0);
        mod.addFunction(main);
        // Predeclare user functions
        for (Stmt.Fun f : funs) mod.addFunction(new Function(f.name.lexeme, f.params.size(), 0));
        // Compile main body
        push(main);
        emit(ENTER, 0);
        for (Stmt s : program) {
            if (!(s instanceof Stmt.Fun)) stmt(s);
        }
        emit(LEAVE);
        emit(RET);
        pop();
        // Compile each function
        for (Stmt.Fun f : funs) {
            Function decl = mod.funcs.get(mod.findFunctionByName(f.name.lexeme));
            push(decl);
            // Params occupy first slots
            for (int i = 0; i < f.params.size(); i++) {
                String p = f.params.get(i).lexeme;
                allocLocal(p);
            }
            emit(ENTER, 0);
            for (Stmt st : f.body) stmt(st);
            // Implicit return nil
            emit(LEAVE);
            emit(RET);
            pop();
        }
        return mod;
    }

    private FuncCtx cur() { return stack.peek(); }
    private void push(Function f) { stack.push(new FuncCtx(f)); }
    private void pop() { stack.pop(); }

    private int allocLocal(String name) {
        FuncCtx c = cur();
        Integer idx = c.locals.get(name);
        if (idx != null) return idx;
        int n = c.nextLocal++;
        c.locals.put(name, n);
        c.fn.nlocals = Math.max(c.fn.nlocals, c.nextLocal);
        return n;
    }

    private void emit(Opcode op) { cur().fn.code.add(new Instr(op)); }
    private void emit(Opcode op, int a) { cur().fn.code.add(new Instr(op, a)); }
    private void emit2(Opcode op, int a, int b) { cur().fn.code.add(new Instr(op, a, b)); }

    // Statements
    private void stmt(Stmt s) {
        if (s instanceof Stmt.Let v) {
            int slot = allocLocal(v.name.lexeme);
            if (v.init != null) {
                expr(v.init);
                emit(STORE, slot);
            }
            return;
        }
        if (s instanceof Stmt.ExprStmt es) {
            expr(es.expr);
            emit(POP);
            return;
        }
        if (s instanceof Stmt.Print p) {
            expr(p.value);
            emit(PRINT);
            return;
        }
        if (s instanceof Stmt.Block b) {
            // Simple approach: locals remain allocated (no stack frame shrinking here)
            for (Stmt st : b.stmts) stmt(st);
            return;
        }
        if (s instanceof Stmt.If iff) {
            expr(iff.cond);
            int jmpFalseAt = cur().fn.code.size();
            emit(JMP_IF_FALSE, 0); // patch later
            stmt(iff.thenBranch);
            int jmpEndAt = -1;
            if (iff.elseBranch != null) {
                jmpEndAt = cur().fn.code.size();
                emit(JMP, 0);
            }
            // patch jmpFalse
            patchRel(jmpFalseAt, cur().fn.code.size() - (jmpFalseAt + 1));
            if (iff.elseBranch != null) {
                stmt(iff.elseBranch);
                patchRel(jmpEndAt, cur().fn.code.size() - (jmpEndAt + 1));
            }
            return;
        }
        if (s instanceof Stmt.While w) {
            int loopStart = cur().fn.code.size();
            expr(w.cond);
            int jmpOutAt = cur().fn.code.size();
            emit(JMP_IF_FALSE, 0);
            stmt(w.body);
            int back = loopStart - (cur().fn.code.size() + 1);
            emit(JMP, back);
            patchRel(jmpOutAt, cur().fn.code.size() - (jmpOutAt + 1));
            return;
        }
        if (s instanceof Stmt.Return r) {
            if (r.value != null) expr(r.value); else emit(CONST, addConst(null));
            emit(LEAVE);
            emit(RET);
            return;
        }
        if (s instanceof Stmt.Fun) {
            // Already predeclared/compiled separately
            return;
        }
        throw new IllegalArgumentException("Unknown Stmt: " + s.getClass());
    }

    private void patchRel(int at, int rel) {
        Instr old = cur().fn.code.get(at);
        cur().fn.code.set(at, new Instr(old.op, rel, old.b));
    }

    // Expressions
    private void expr(Expr e) {
        if (e instanceof Expr.Literal l) {
            int k = addConst(l.value);
            emit(CONST, k);
            return;
        }
        if (e instanceof Expr.Variable v) {
            Integer idx = cur().locals.get(v.name.lexeme);
            if (idx == null) {
                // treat as function ref (direct name), push as const? We'll compile calls directly.
                // Fallback: load undefined local -> will be runtime error; for now push nil.
                emit(CONST, addConst(null));
            } else {
                emit(LOAD, idx);
            }
            return;
        }
        if (e instanceof Expr.Unary u) {
            expr(u.right);
            switch (u.op.type) {
                case BANG -> emit(NOT);
                case MINUS -> { int z = addConst(0); emit(CONST, z); emit(SUB); } // 0 - x
                default -> throw new RuntimeException("Unsupported unary: " + u.op.lexeme);
            }
            return;
        }
        if (e instanceof Expr.Binary b) {
            expr(b.left);
            expr(b.right);
            switch (b.op.type) {
                case PLUS -> emit(ADD);
                case MINUS -> emit(SUB);
                case STAR -> emit(MUL);
                case SLASH -> emit(DIV);
                
                case EQUAL_EQUAL -> emit(EQ);
                case BANG_EQUAL -> emit(NE);
                case LESS -> emit(LT);
                case LESS_EQUAL -> emit(LE);
                case GREATER -> emit(GT);
                case GREATER_EQUAL -> emit(GE);
                case OR_OR -> { // a || b  => (a) dup; jmp_if_false eval_b; pop; load true
                    // For bytecode simplicity, compute a || b as: (a) (b) OR at runtime via truthiness in VM (not implemented yet).
                    // Here we just leave both evaluated and not emit special op; interpreter/VM to define truthiness later.
                    // Placeholder: emit NEQ against zero? keep EQ/NE for ints only.
                    // Keep as-is: no extra op; would require VM semantics.
                }
                case AND_AND -> { /* same note as above */ }
                default -> throw new RuntimeException("Unsupported binary: " + b.op.lexeme);
            }
            return;
        }
        if (e instanceof Expr.Grouping g) {
            expr(g.expr);
            return;
        }
        if (e instanceof Expr.Call c) {
            // Direct calls only: callee must be a Variable
            if (c.callee instanceof Expr.Variable v) {
                int fidx = mod.findFunctionByName(v.name.lexeme);
                if (fidx < 0) throw new RuntimeException("Unknown function '" + v.name.lexeme + "'");
                for (Expr a : c.args) expr(a);
                emit2(CALL, fidx, c.args.size());
                return;
            }
            throw new RuntimeException("Only direct calls by name are supported in bytecode compiler");
        }
        throw new IllegalArgumentException("Unknown Expr: " + e.getClass());
    }

    private int addConst(Object v) { return mod.addConst(v); }
}