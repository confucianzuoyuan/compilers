package Frag;

// 程序段，以 .text 开头
public class ProcFrag extends Frag {
    public Frame.Frame frame; // 程序段的帧
    public Tree.Stm body; // 程序段内的程序体

    public ProcFrag(Tree.Stm body, Frame.Frame f) {
        this.body = body;
        frame = f;
    }
}
