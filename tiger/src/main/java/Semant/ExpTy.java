package Semant;

import Translate.Exp;
import Types.Type;

// 带返回值类型的表达式类
public class ExpTy {
    Exp exp; // 表达式体
    Type ty; // 表达式返回值类型

    ExpTy(Exp e, Type t) {
        exp = e;
        ty = t;
    }
}
