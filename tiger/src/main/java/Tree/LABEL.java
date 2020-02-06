package Tree;

import Temp.Label;

// LABEL(n) 定义名字n的常数值为当前机器代码的地址。这类似于汇编语言中的标号定义。
// 值NAME(n)可能是转移或者调用等操作的目标。
public class LABEL extends Stm {
    public Label label;

    public LABEL(Label l) {
        label = l;
    }

    public ExpList kids() {
        return null;
    }

    public Stm build(ExpList kids) {
        return this;
    }
}
