/*
 * Decompiled with CFR 0.152.
 */
package com.basebackend.nacos.refresh;

public interface SharedConfigListener {
    public String getDataIdPattern();

    public String getGroup();

    public void onChange(String var1, String var2, String var3);
}
