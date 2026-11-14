# Terminal (טרמינל)

`com.future.terminal` — Root shell terminal.

Executes each command via `su -c <command>` (not a persistent pty), with `cwd` tracked in-app and injected as `cd "$cwd"; <command>`. Scrolling monospace output log.
