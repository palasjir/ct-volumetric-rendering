package palasjir.kglm

import com.hackoeur.jglm.Mat4
import com.hackoeur.jglm.Vec4

operator fun Mat4.times(right: Mat4): Mat4 = this.multiply(right)
operator fun Mat4.times(right: Vec4): Vec4 = this.multiply(right)
