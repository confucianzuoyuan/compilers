package Translate;

import Symbol.Symbol;
import Util.BoolList;

public class Level {
    public Level parent; // 上一层Level
    Frame.Frame frame; // 层中对应的帧
    public AccessList formals = null; // 参数列表，应该是形式参数

    // 构造函数，代入上一层
    public Level(Level parent, Symbol name, BoolList fmls) {
        this.parent = parent;
        BoolList bl = new BoolList(true, fmls);
        this.frame = parent.frame.newFrame(new Temp.Label(name), bl);
        for (Frame.AccessList f = frame.formals; f != null; f = f.next) {
            this.formals = new AccessList(new Access(this, f.head), this.formals);
        }
    }

    // 新建一个没有上一层的层，就是顶层
    public Level(Frame.Frame frm) {
        this.frame = frm;
        this.parent = null;
    }

    // 返回这一层的静态链，作为函数来说就是寄存器$a0
    // 本层的静态链就是形式参数列表的第一个参数
    public Access staticLink() {
        return formals.head;
    }

    // 分配存储空间
    public Access allocLocal(boolean escape) {
        return new Access(this, frame.allocLocal(escape));
    }
}
