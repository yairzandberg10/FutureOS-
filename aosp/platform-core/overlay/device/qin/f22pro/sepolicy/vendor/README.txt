vendor sepolicy: אין כללים כרגע. init כותב ל-/proc/sys/vm ול-cpuset בדומיין init, ואין צורך בכללים.
אם נבחר node טעינה (568) ב-sysfs, להוסיף label ל-node ב-genfs_contexts של ה-vendor.
