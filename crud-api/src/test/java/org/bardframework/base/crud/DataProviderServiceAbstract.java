package org.bardframework.base.crud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Created by Sama-PC on 08/05/2017.
 */
public abstract class DataProviderServiceAbstract<M extends BaseModelAbstract<I>, C extends BaseCriteriaAbstract<I>, D, S extends BaseServiceAbstract<M, C, D, R, I, U>, R extends BaseRepository<M, C, I, U>, I extends Number & Comparable<I>, U> extends DataProviderRepositoryAbstract<M, C, R, I, U> {

    @Autowired
    protected S service;

    @Override
    public final M getUnsavedModel(U user) {
        return this.toModel(this.getUnsavedDto(user), user);
    }

    public final List<D> getUnsavedDtos(long count, U user) {
        List<D> models = new ArrayList<>();
        while (models.size() < count) {
            D dto = getUnsavedDto(user);
            if (models.stream().noneMatch(element -> this.isDuplicate(element, dto, user))) {
                models.add(dto);
            }
        }
        return models;
    }

    /**
     * use {@link #isDuplicate(M, M, U)} in default implementation
     *
     * @param first
     * @param second
     * @param user
     * @return
     */
    protected boolean isDuplicate(D first, D second, U user) {
        return this.isDuplicate(this.toModel(first, user), this.toModel(second, user), user);
    }

    /**
     * @param first
     * @param second
     * @param user
     * @return
     */
    @Override
    protected boolean isDuplicate(M first, M second, U user) {
        return super.isDuplicate(first, second, user);
    }

    //Dto...
    protected M toModel(D dto, U user) {
        return service.onSave(dto, user);
    }

    public abstract D getUnsavedDto(U user);

    //TODO erroneous data in some cases (when some fields can't update)
    public D getDto(U user) {
        return this.getUnsavedDto(user);
    }

    public final D getInvalidDto(U user) {
        return this.makeInvalid(this.getDto(user));
    }

    public final D getUnsavedInvalidDto(U user) {
        return makeInvalid(this.getUnsavedDto(user));
    }
    //...Dto

    public void assertEqualSave(M model, D dto) {
        this.assertEqualUpdate(model, dto);
    }

    public abstract void assertEqualUpdate(M model, D dto);

    /**
     * save <count>count<count/> new entities
     *
     * @param count
     * @return saved entities
     */
    @Transactional
    @Override
    public List<M> saveNew(long count, U user) {
        List<M> unsavedModels = new ArrayList<>();
        for (long i = 0; i < count; i++) {
            unsavedModels.add(service.save(getUnsavedDto(user), user));
        }
        return unsavedModels;
    }

    /**
     * @param unsavedDto
     * @param user
     * @param validateFunction
     * @return a model that pass <code>validateFunction</code>, saved <code>unsavedDto</code> otherwise.
     */
    public M getOrSave(D unsavedDto, U user, Function<M, Boolean> validateFunction) {
        M model = this.getModel(validateFunction, user);
        return null == model ? service.save(unsavedDto, user) : model;
    }

    protected abstract D makeInvalid(D dto);
}
