package org.bardframework.base.activable;

import org.bardframework.base.crud.BaseModelAbstract;
import org.bardframework.base.crud.BaseRepository;

import java.io.Serializable;

public interface ActivableEntityService<M extends BaseModelAbstract<I>, R extends ActivableEntityRepository<I, U> & BaseRepository<M, ?, I, U>, I extends Serializable, U> {

    default M enable(I id, U user) {
        this.getRepository().setEnable(id, true, user);
        return this.getRepository().get(id, user);
    }

    default M disable(I id, U user) {
        this.getRepository().setEnable(id, false, user);
        return this.getRepository().get(id, user);
    }

    R getRepository();
}
