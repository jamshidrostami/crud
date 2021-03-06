package org.bardframework.base.crud;

import org.bardframework.commons.utils.AssertionUtils;
import org.bardframework.commons.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by vahid on 1/17/17.
 */
public abstract class BaseServiceAbstract<M extends BaseModelAbstract<I>, C extends BaseCriteriaAbstract<I>, D, R extends BaseRepository<M, C, I, U>, I extends Serializable, U> implements BaseService<M, C, D, I, U> {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    protected final Class<M> modelClazz;
    protected final Class<C> criteriaClazz;
    @Autowired
    protected R repository;

    public BaseServiceAbstract() {
        ParameterizedType parameterizedType = null;
        Class<?> targetClazz = this.getClass();
        while (!(null != parameterizedType && parameterizedType.getActualTypeArguments().length >= 2) && null != targetClazz) {
            parameterizedType = targetClazz.getGenericSuperclass() instanceof ParameterizedType ? (ParameterizedType) targetClazz.getGenericSuperclass() : null;
            targetClazz = targetClazz.getSuperclass();
        }
        try {
            this.modelClazz = (Class<M>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            this.criteriaClazz = (Class<C>) parameterizedType.getActualTypeArguments()[1];
        } catch (Exception e) {
            this.LOGGER.debug("can't determine class from generic type!", e);
            throw new IllegalArgumentException("can't determine class from generic type!", e);
        }
    }

    public M getEmptyModel() {
        try {
            return modelClazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            this.LOGGER.error("can't instantiate model class using empty constructor {}", this.modelClazz, e);
            throw new IllegalArgumentException("can't instantiate model class using empty constructor" + this.modelClazz, e);
        }
    }

    public C getEmptyCriteria() {
        C criteria;
        try {
            criteria = criteriaClazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            this.LOGGER.error("can't instantiate criteria class using empty constructor {}", this.criteriaClazz, e);
            throw new IllegalArgumentException("can't instantiate criteria class using empty constructor" + this.criteriaClazz, e);
        }
        return criteria;
    }

    /**
     * get by id
     *
     * @param id
     * @return
     */
    @Override
    public M get(I id, U user) {
        return this.getRepository().get(id, user);
    }

    public List<M> get(List<I> ids, U user) {
        return this.getRepository().get(ids, user);
    }

    /**
     * get all data match with given <code>criteria</code>
     *
     * @param criteria
     * @return
     */
    public List<M> get(C criteria, U user) {
        return this.getRepository().get(criteria, user);
    }

    /**
     * @param criteria
     * @return
     */
    public M getOne(C criteria, U user) {
        return this.getRepository().getOne(criteria, user);
    }

    @Transactional
    public long delete(C criteria, U user) {
        List<M> models = this.repository.get(criteria, user);
        if (CollectionUtils.isEmpty(models)) {
            return 0;
        }
        for (M model : models) {
            this.preDelete(model, user);
        }
        /*
        call directDelete(List) instead of delete(List).
        maybe some joined part has been deleted in preDelete (like status change)
         */
        long deletedCount = repository.directDelete(models.stream().map(M::getId).collect(Collectors.toList()), user);
        if (deletedCount > 0) {
            for (M model : models) {
                this.postDelete(model, user);
            }
        }
        if (models.size() != deletedCount) {
            LOGGER.warn("deleting with criteria, expect delete {} item(s), but {} deleted.", models.size(), deletedCount);
        }
        return deletedCount;
    }

    @Transactional
    public long delete(List<I> ids, U user) {
        C criteria = this.getEmptyCriteria();
        criteria.setIds(ids);
        return this.delete(criteria, user);
    }

    /**
     * delete data with given id
     *
     * @param id   identifier of data that must be delete
     * @param user
     * @return count of deleted data
     */
    @Transactional
    @Override
    public long delete(I id, U user) {
        C criteria = this.getEmptyCriteria();
        criteria.setIds(Collections.singletonList(id));
        return this.delete(criteria, user);
    }

    /**
     * execute before deleting data
     *
     * @param model
     */
    protected void preDelete(M model, U user) {
    }

    /**
     * execute after deleting data
     *
     * @param deletedModel
     */
    protected void postDelete(M deletedModel, U user) {
    }

    /**
     * save new data
     *
     * @param dto
     * @param user
     * @return saved data model
     */
    @Transactional
    @Override
    public M save(D dto, U user) {
        AssertionUtils.notNull(dto, "dto cannot be null.");
        this.preSave(dto, user);
        M model = this.getRepository().save(this.onSave(dto, user), user);
        this.postSave(model, dto, user);
        return this.getRepository().get(model.getId(), user);
    }

    /**
     * save new data
     *
     * @param dtos
     * @param user
     * @return saved data models
     */
    @Transactional
    public List<M> save(List<D> dtos, U user) {
        AssertionUtils.notEmpty(dtos, "dtos cannot be null or empty.");
        List<M> list = new ArrayList<>();
        for (D dto : dtos) {
            this.preSave(dto, user);
            list.add(this.onSave(dto, user));
        }
        list = this.getRepository().save(list, user);
        if (list.size() != dtos.size()) {
            throw new IllegalStateException("invalid save operation, save " + dtos.size() + " dtos, but result size is " + list.size());
        }
        for (int i = 0; i < list.size(); i++) {
            this.postSave(list.get(i), dtos.get(i), user);
        }
        return list;
    }

    /**
     * converting dto to model for saving
     * specify
     *
     * @param dto
     * @param user
     */
    protected abstract M onSave(D dto, U user);

    protected void preSave(D dto, U user) {
    }

    protected void postSave(M savedModel, D dto, U user) {
    }

    @Transactional
    @Override
    public M update(I id, D dto, U user) {
        M model = this.getRepository().get(id, user);
        this.preUpdate(model, dto, user);
        this.getRepository().update(this.onUpdate(dto, model, user), user);
        this.postUpdate(model, dto, user);
        return this.getRepository().get(model.getId(), user);
    }

    protected abstract M onUpdate(D dto, M previousModel, U user);

    protected void preUpdate(M previousModel, D dto, U user) {
    }

    protected void postUpdate(M updatedModel, D dto, U user) {
    }

    @Override
    public DataTableModel<M> filter(C criteria, U user) {
        return this.getRepository().filter(criteria, user);
    }

    public List<I> getIds(C criteria, U user) {
        return this.getRepository().getIds(criteria, user);
    }

    public long getCount(C criteria, U user) {
        return this.getRepository().getCount(criteria, user);
    }

    public boolean isExist(C criteria, U user) {
        return this.getRepository().isExist(criteria, user);
    }

    public boolean isNotExist(C criteria, U user) {
        return this.getRepository().isNotExist(criteria, user);
    }

    public R getRepository() {
        return repository;
    }

    public Logger getLogger() {
        return LOGGER;
    }
}