package main

import (
	"fmt"
	"os"
	"os/user"

	"./repl"
)

func main() {
	user, err := user.Current()
	if err != nil {
		panic(err)
	}
	fmt.Printf("你好 %s! 这是左元用Golang写的解释器！\n", user.Username)
	fmt.Printf("开始写代码吧！\n")
	repl.Start(os.Stdin, os.Stdout)
}
