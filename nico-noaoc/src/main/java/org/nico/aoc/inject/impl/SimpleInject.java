package org.nico.aoc.inject.impl;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.nico.aoc.aspect.buddy.AspectBuddy;
import org.nico.aoc.inject.BookInject;
import org.nico.aoc.inject.type.TypeConvert;
import org.nico.aoc.scan.annotations.Label;
import org.nico.aoc.util.reflect.FieldUtils;
import org.nico.aoc.util.reflect.MethodUtils;
import org.nico.asm.buddy.ASMClassBuddy;
import org.nico.asm.contains.entity.ASMClassEntity;
import org.nico.asm.contains.entity.ASMMethodEntity;
import org.nico.asm.contains.entity.ASMParameterEntity;
import org.nico.log.Logging;
import org.nico.log.LoggingHelper;
import org.nico.util.collection.CollectionUtils;

/** 
 * Ordinary Inject implements
 * @author nico
 * @version 创建时间：2017年12月13日 下午9:29:21
 */
public class SimpleInject implements BookInject{

	private Logging logging = LoggingHelper.getLogging("NOAOC");

	private TypeConvert typeConvert;

	public SimpleInject(){
		typeConvert = new TypeConvert();
	}

	@Override
	public void parameterSetInject(Object object, Map<Object, Object> paramSource) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		/**
		 * Gets the real object from the proxy object.
		 */
		object = AspectBuddy.getTargetObject(object);
		
		Class<?> clazz = object.getClass();
		if(null != paramSource && ! paramSource.isEmpty()){
			for(Entry<Object, Object> entry: paramSource.entrySet()){
				Field field = null;
				try {
					field = FieldUtils.getField((String) entry.getKey(), clazz);
				} catch (NoSuchFieldException e) {
//					logging.warning(e.getMessage());
				}
				if(field != null){
					Object value = ( ! (entry.getValue() instanceof String) )? entry.getValue() : typeConvert.convert(field.getType(), String.valueOf(entry.getValue()));
					if(Modifier.isStatic(field.getModifiers()) || field.isAnnotationPresent(Label.class)){
						field.setAccessible(true);
						field.set(object, value);
						logging.debug("Inject into " + object.getClass().getName() + " for field " + field.getName() + " by accessible = true");
						field.setAccessible(false);
					}else{
						Method method = MethodUtils.getSetterMethod(field, clazz);
						if(method != null){
							logging.debug("Inject into " + object.getClass().getName() + " for field " + field.getName() + " by method " + method.getName() + " on setter");
							MethodUtils.invoke(method, object, value);
						}
					}
				}else{
					Method method = MethodUtils.getSetterMethod((String)entry.getKey(), clazz);
					if(method != null){
						Object value = ( ! (entry.getValue() instanceof String) )? entry.getValue() : typeConvert.convert(method.getParameterTypes()[0], String.valueOf(entry.getValue()));
						logging.debug("Inject into " + object.getClass().getName() + " by method " + method.getName() + " on setter");
						MethodUtils.invoke(method, object, value);
					}
				}
			}
		}
	}

	@Override
	public <T> T parameterConstructorInject(Class<T> clazz, Map<Object, Object> paramSource) throws IOException {
		T target = null;
		ASMClassEntity classEntity = ASMClassBuddy.getClassEntity(clazz);
		List<ASMMethodEntity> entities = classEntity.getConstructionMethods();
		int parameterCount = parameterCount(paramSource.keySet());
		ASMMethodEntity hitEntity = getConstructorEntity(entities, paramSource, parameterCount);
		if(hitEntity != null){
			try {
				if(parameterCount != 0){
					Object[] objects = transferParametersBuilder(
							hitEntity.getParameters(),
							paramSource,
							hitEntity.getMethod().getParameterTypes());
					target = (T) ((Constructor<T>)hitEntity.getMethod()).newInstance(objects);
				}else{
					target = clazz.newInstance();
				}
				List<String> types = new ArrayList<String>();
				if(hitEntity.getMethod().getParameterCount() > 0){
					for(Type type: hitEntity.getMethod().getGenericParameterTypes()){
						types.add(type.getTypeName());
					}
				}
				logging.debug("Construction " + clazz.getName() + " By " + types);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				logging.error(e);
			}
		}
		return target;
	}

	/**
	 * Get constructorEntity from entities
	 * 
	 * @param entities constructorEntities that get by asm
	 * @param parameters inject params
	 * @param parameterCount params's count
	 * @return ConstructorEntity hit Entity
	 */
	private ASMMethodEntity getConstructorEntity(List<ASMMethodEntity> entities, Map<Object, Object> parameters, int parameterCount){
		ASMMethodEntity hitEntity = null;
		if(CollectionUtils.isNotBlank(entities)){
			for(ASMMethodEntity entity: entities){
				Set<String> methodParams = new HashSet<String>();
				String[] asmParameters = entity.getParameters();
				if(asmParameters != null && asmParameters.length > 0){
					for(String asmParameter: asmParameters){
						methodParams.add(asmParameter);
					}
				}
				int entityParameterCount = parameterCount(asmParameters);
				if(parameterCount == 0){
					if(parameterCount == entityParameterCount){
						hitEntity = entity;
					}
					if(hitEntity != null) break;
				}else{
					if(parameterCount >= entityParameterCount){
						if(parameters.keySet().containsAll(methodParams)){
							if(hitEntity != null && parameterCount(hitEntity.getParameters()) < parameterCount(entity.getParameters())){
								hitEntity = entity;
							}
							if(hitEntity == null){
								hitEntity = entity;
							}
						}
					}
				}
			}
		}
		return hitEntity;
	}

	/**
	 * Get count of map
	 * @param parameters map
	 * @return count of the map
	 */
	private int parameterCount(String[] parameters){
		if(parameters == null || parameters.length == 0) return 0;
		return parameters.length;
	}
	
	private int parameterCount(Set<?> parameters){
		if(parameters == null || parameters.size() == 0) return 0;
		return parameters.size();
	}

	/**
	 * Building transfer parameters of the method or constructor
	 * 
	 * @param paramMap method or constructor 's params
	 * @param paramSource the params from external
	 * @param types method or constructor 's types
	 * @return transfer parameters
	 */
	private Object[] transferParametersBuilder(String[] parameters, Map<Object, Object> paramSource, Class<?>[] types){
		Object[] objects = new Object[types.length];
		for(int index = 0; index < objects.length; index ++){
			Object pending =  paramSource.get(parameters[index]);
			if(pending != null) objects[index] = pending instanceof String ? typeConvert.convert(types[index], (String)pending) : pending;
		}
		return objects;
	}
	
	/**
	 * Parameters Fileter
	 * It is used to filter injection parameters and do some things before injection.
	 * 
	 * @param parameters be injected parameters
	 */
	private void filterParameters(Map<Object, Object> parameters){
		if(parameters != null && parameters.size() > 0){
			for(Entry<Object, Object> entry: parameters.entrySet()){
				/**
				 * Gets the real object from the proxy object.
				 */
				entry.setValue(AspectBuddy.getTargetObject(entry.getValue()));
			}
		}
	}

}
