package Frame;

import Temp.Label;
import Temp.Temp;
import Temp.TempMap;
import Util.BoolList;

public abstract class Frame implements TempMap {
    // 建立新帧(名称、参数、逃逸信息)
    // newFrame(f, l) 表示给带有 k 个形式参数的函数 f 创建一个新栈帧
    // l 是 k 个布尔量组成的一个表，true 表示参数是逃逸的，false 表示参数不是逃逸的
    public abstract Frame newFrame(Label name, BoolList formals);

    public Label name; // 名称
    public AccessList formals = null; // 本地变量(局部变量、参数)列表

    public abstract Access allocLocal(boolean escape); // 分配新本地变量(是否逃逸)

    public abstract Tree.Exp externalCall(String func, Tree.ExpList args); // 外部函数

    public abstract Temp FP(); // Frame Pointer 帧指针

    public abstract Temp SP(); // Stack Pointer 栈指针

    public abstract Temp RA(); // return address 返回地址

    public abstract Temp RV(); // return value 返回值

    public abstract java.util.HashSet<Temp> registers(); // 寄存器列表

    public abstract Tree.Stm procEntryExit1(Tree.Stm body); // 添加额外函数调用指令

    public abstract String string(Label label, String value);
}
