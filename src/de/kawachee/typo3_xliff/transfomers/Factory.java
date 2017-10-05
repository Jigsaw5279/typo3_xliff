package de.kawachee.typo3_xliff.transfomers;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Factory {

    @NotNull
    public static Transformer build(String type) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<?> className = Class.forName("de.kawachee.typo3_xliff.transfomers." + type);
        Constructor ctr = className.getConstructor();

        return (Transformer) ctr.newInstance();
    }
}
