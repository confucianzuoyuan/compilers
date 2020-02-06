package Tree;

// ESEQ(s,e) 先计算语句s以形成其副作用，然后计算e作为此表达式的结果。
public class ESEQ extends Exp {
    public Stm stm;
    public Exp exp;

    public ESEQ(Stm s, Exp e) {
        super(null);
        stm = s;
        exp = e;
    }

    public ExpList kids() {
        throw new Error("kids() not applicable to ESEQ");
    }

    public Exp build(ExpList kids) {
        throw new Error("build() not applicable to ESEQ");
    }
}
