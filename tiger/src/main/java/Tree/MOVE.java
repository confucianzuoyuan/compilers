package Tree;

// MOVE(TEMP t, e) 计算e并将结果送入临时单元t。
// MOVE(MEM(e1),e2) 计算e1，由它生成地址a。
// 然后计算e2，并将计算结果存储在从地址a开始的
// wordSize个字节的存储单元中。
public class MOVE extends Stm {
    public Exp dst, src;

    public MOVE(Exp d, Exp s) {
        dst = d;
        src = s;
    }

    public ExpList kids() {
        if (dst instanceof MEM) {
            return new ExpList(((MEM)dst).exp, new ExpList(src, null));
        } else {
            return new ExpList(src, null);
        }
    }

    public Stm build(ExpList kids) {
        if (dst instanceof MEM) {
            return new MOVE(new MEM(kids.head), kids.tail.head);
        } else {
            return new MOVE(dst, kids.head);
        }
    }
}
