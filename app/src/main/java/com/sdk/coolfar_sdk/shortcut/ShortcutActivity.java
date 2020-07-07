package com.sdk.coolfar_sdk.shortcut;

import android.app.Activity;
import android.os.Bundle;

/**
 * 通过指定多个Launcher入口实现
 * <p/>
 * Created by xuyisheng on 15/10/30.
 * Version 1.0
 */
public class ShortcutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.xys.shortcut_lib.R.layout.shortcut_main);
    }
}
