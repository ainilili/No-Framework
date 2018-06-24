package org.nico.asm.buddy;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nico.asm.contains.entity.ASMClassEntity;
import org.nico.asm.contains.entity.ASMConstructionEntity;
import org.nico.asm.contains.entity.ASMMethodEntity;
import org.nico.asm.contains.entity.ASMParameterEntity;
import org.nico.asm.util.TypeUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/** 
 * 
 * @author nico
 * @version createTime：2018年5月3日 下午9:53:35
 */

public class ASMMethodBuddy {

	public static ASMMethodEntity getMethodEntity(final Method method) throws IOException{
		final ASMMethodEntity methodEntity = new ASMMethodEntity();

		methodEntity.setMethodName(method.getName());

		Class<?>[] types = method.getParameterTypes();

		final Type[] asmTypes = TypeUtils.convertASMTypes(types);


		ClassReader classReader = new ClassReader(method.getDeclaringClass().getName());
		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5) {

			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature,
					String[] exceptions) {
				Type[] paramTypes = Type.getArgumentTypes(desc);

				if(!name.equals("<clinit>") && name.equals(methodEntity.getMethodName()) && Arrays.equals(paramTypes, asmTypes) && methodEntity.getNormalParameters() == null){

					methodEntity.setMethod(method);

					final List<ASMParameterEntity> normalParameters = new ArrayList<ASMParameterEntity>();

					MethodVisitor mv = new MethodVisitor(Opcodes.ASM5) {

						@Override
						public void visitLocalVariable(String name, String desc, String signature, Label start, Label end,
								int index) {
							if(! name.equals("this")){
								normalParameters.add(new ASMParameterEntity(name, signature));
							}
							super.visitLocalVariable(name, desc, signature, start, end, index);
						}

						@Override
						public void visitEnd() {
							methodEntity.setNormalParameters(normalParameters);
							super.visitEnd();
						}



					};

					return mv;
				}else{
					return super.visitMethod(access, name, desc, signature, exceptions);
				}

			}

		};
		classReader.accept(visitor, ClassReader.EXPAND_FRAMES);

		return methodEntity;
	}

	public static ASMConstructionEntity getConstructorEntity(final Constructor<?> constructor) throws IOException{
		final ASMConstructionEntity methodEntity = new ASMConstructionEntity();

		methodEntity.setMethodName(constructor.getName());

		Class<?>[] types = constructor.getParameterTypes();

		final Type[] asmTypes = new Type[types == null ? 0 : types.length];
		if(types != null){
			int index = 0;
			for(Class<?> clazz: types){
				asmTypes[index ++] = Type.getType(clazz);
			}
		}


		ClassReader classReader = new ClassReader(constructor.getDeclaringClass().getName());
		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5) {

			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature,
					String[] exceptions) {
				Type[] paramTypes = Type.getArgumentTypes(desc);
				if(name.equals("<init>") && Arrays.equals(paramTypes, asmTypes) && methodEntity.getNormalParameters() == null){

					methodEntity.setConstructor(constructor);
					
					final List<ASMParameterEntity> normalParameters = new ArrayList<ASMParameterEntity>();
					MethodVisitor mv = new MethodVisitor(Opcodes.ASM5) {

						@Override
						public void visitLocalVariable(String name, String desc, String signature, Label start, Label end,
								int index) {
							if(! name.equals("this")){
								normalParameters.add(new ASMParameterEntity(name, signature));
							}
							super.visitLocalVariable(name, desc, signature, start, end, index);
						}

						@Override
						public void visitEnd() {
							methodEntity.setNormalParameters(normalParameters);
							super.visitEnd();
						}

					};

					return mv;
				}else{
					return super.visitMethod(access, name, desc, signature, exceptions);
				}

			}

		};
		classReader.accept(visitor, ClassReader.EXPAND_FRAMES);

		return methodEntity;
	}
}
