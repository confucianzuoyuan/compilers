package Tree;

// BINOP(o, e1, e2) 对操作数e1、e2施加二元操作符o表示的操作，子表达式e1的计算
// 先于e2。整型算数操作符是PLUS、MINUS、MUL、DIV；整型按位逻辑操作符是AND、OR、XOR；
// 整型逻辑移位操作符是LSHIFT、RSHIFT；整型算术右移操作符是ARSHIFT。Tiger语言
// 没有逻辑操作符，这里出现逻辑操作符是因为中间语言是独立于任何源语言的，并且在
// 实现Tiger语言的其他特征时可能会需要逻辑操作。
public class BINOP extends Exp {
    public int binop;
    public Exp left, right;

    public BINOP(int b, Exp l, Exp r) {
        super(null);
        binop = b;
        left = l;
        right = r;
    }

    public final static int PLUS = 0, MINUS = 1, MUL = 2, DIV = 3, AND = 4, OR = 5, LSHIFT = 6, RSHIFT = 7, ARSHIFT = 8, XOR = 9;

    public ExpList kids() {
        return new ExpList(left, new ExpList(right, null));
    }

    public Exp build(ExpList kids) {
        return new BINOP(binop, kids.head, kids.tail.head);
    }
}
