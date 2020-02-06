package Semant;

import Temp.Label;
import Translate.Level;
import Types.RECORD;
import Types.Type;

public class FuncEntry extends Entry {
    RECORD paramlist; // 参数列表
    Type returnTy; // 返回值类型
    public Level level; // 函数的层
    public Label label; // 函数的标签的名称

    public FuncEntry(Level level, Label label, RECORD p, Type rt) {
        paramlist = p;
        returnTy = rt;
        this.level = level;
        this.label = label;
    }
}
