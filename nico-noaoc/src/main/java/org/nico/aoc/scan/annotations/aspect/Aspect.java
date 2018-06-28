package org.nico.aoc.scan.annotations.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.nico.aoc.scan.entity.AspectType;

/** 
 * This annotation ACTS on the Class, representing a section.
 * 
 * @author nico
 * @version createTime：2018年3月7日 下午2:31:14
 */

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Aspect {

	AspectType type() default AspectType.JDK_PROXY;
}
