package Semant;

import Symbol.Symbol;
import Symbol.Table;
import Tiger.errormsg.ErrorMsg;
import Translate.Level;
import Types.*;
import Util.BoolList;

// 环境，也就是符号表
public class Env {
    Table vEnv = null; // 入口符号表，记录了变量和函数的入口
    Table tEnv = null; // 类型符号表，记录了变量的类型
    Level root = null; // 最顶层，root层
    ErrorMsg errorMsg = null; // 传入 main 函数的层，添加库函数时使用
    // 记录标准库函数的集合
    java.util.HashSet<Symbol> stdFuncSet = new java.util.HashSet<Symbol>();

    Env(ErrorMsg err, Level l) {
        errorMsg = err;
        root = l;
        // 初始化两个环境（符号表）
        initTEnv();
        initVEnv();
    }

    public void initTEnv() {
        // 初始化类型符号表，加入 int 和 string 这两个默认类型
        tEnv = new Table();
        tEnv.put(Symbol.symbol("int"), new INT());
        tEnv.put(Symbol.symbol("string"), new STRING());
    }

    public void initVEnv() {
        // 初始化入口符号表，添加标准库函数，库函数的实现为 runtime.s 中的汇编代码
        vEnv = new Table();

        Symbol sym = null;
        RECORD formals = null;
        Type result = null;
        Level level = null;

        sym = Symbol.symbol("allocRecord");
        formals = new RECORD(Symbol.symbol("size"), new INT(), null);
        result = new INT();
        level = new Level(root, sym, new BoolList(true, null));
        vEnv.put(sym, new StdFuncEntry(level, new Temp.Label(sym), formals, result));
        stdFuncSet.add(sym);

        sym = Symbol.symbol("initArray");
        formals = new RECORD(Symbol.symbol("size"), new INT(), new RECORD(Symbol.symbol("init"), new INT(), null));
        result = new INT();
        level = new Level(root, sym, new BoolList(true, new BoolList(true, null)));
        vEnv.put(sym, new StdFuncEntry(level, new Temp.Label(sym), formals, result));
        stdFuncSet.add(sym);

        sym = Symbol.symbol("print");
        formals = new RECORD(Symbol.symbol("str"), new STRING(), null);
        result = new VOID();
        level = new Level(root, sym, new BoolList(true, null));
        vEnv.put(sym, new StdFuncEntry(level, new Temp.Label(sym), formals, result));
        stdFuncSet.add(sym);

    }
}
