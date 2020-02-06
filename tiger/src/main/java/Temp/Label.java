package Temp;

import Symbol.Symbol;

/**
 * 一个label表示汇编语言中的一个address（地址）
 * */

public class Label {
    public String name;
    private static int count;

    public String toString() {
        return name;
    }

    /**
     * 创建一个名字为n的Label
     * */
    public Label(String n) {
        name = n;
    }

    /**
     * 创建一个具有任意名字的新的label
     * */
    public Label() {
        this("L" + count++);
    }

    /**
     * 创建一个新的label，label的名字和Symbol的名字一样
     * */
    public Label(Symbol s) {
        this(s.toString());
    }
}
