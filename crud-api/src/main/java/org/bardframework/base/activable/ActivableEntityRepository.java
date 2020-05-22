package org.bardframework.base.activable;

public interface ActivableEntityRepository<I extends Comparable<? super I>, U> {

    /**
     * @return true if operation done
     */
    boolean setEnable(I id, boolean enable, U user);
}
