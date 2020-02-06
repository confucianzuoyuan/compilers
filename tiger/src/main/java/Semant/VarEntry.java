package Semant;

import Translate.Access;
import Types.Type;

public class VarEntry extends Entry {
    Type Ty; // 变量类型
    Access acc; // 为变量分配的存储空间
    boolean isFor; // 标记是否为循环变量，用于控制循环的变量，for (i = 0; i < 10; i++)中的i就是循环变量

    public VarEntry(Type ty, Access acc) {
        Ty = ty;
        this.acc = acc;
        this.isFor = false;
    }

    public VarEntry(Type ty, Access acc, boolean isf) {
        Ty = ty;
        this.acc = acc;
        this.isFor = isf;
    }
}
