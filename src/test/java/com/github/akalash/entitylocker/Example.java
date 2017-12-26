package com.github.akalash.entitylocker;

public class Example {

    private void exampleEntityLocker() {
        EntityLocker entityLocker = new EntityLocker();
        entityLocker.globalExclusive(() -> {
            //here global protected code
        });

        entityLocker.exclusive(1, "id", () -> {
            //here protected code by ids(1 and 'id')
        });
    }
}
