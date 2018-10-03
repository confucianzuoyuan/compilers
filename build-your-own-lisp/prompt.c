#include <stdio.h>

// 用户输入的buffer
static char input[2048];

// cc -std=c99 -Wall prompt.c -o prompt
int main(int argc, char** argv) {
    puts("Lispy Version 0.0.0.0.1");
    puts("Press Ctrl+c to Exit\n");

    while (1) {
        fputs("lispy> ", stdout);
        fgets(input, 2048, stdin);
        printf("No you're a %s", input);
    }

    return 0;
}