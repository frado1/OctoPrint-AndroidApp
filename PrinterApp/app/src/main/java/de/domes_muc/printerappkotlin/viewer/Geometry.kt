package de.domes_muc.printerappkotlin.viewer


import android.opengl.Matrix
import de.domes_muc.printerappkotlin.viewer.Geometry.Vector

object Geometry {
    private val OFFSET = 20f

    class Point(val x: Float, val y: Float, val z: Float)

    class Box(minX: Float, maxX: Float, minY: Float, maxY: Float, minZ: Float, maxZ: Float) {

        internal var coordBox = FloatArray(6)

        init {
            coordBox[LEFT] = minX
            coordBox[RIGHT] = maxX
            coordBox[FRONT] = minY
            coordBox[BEHIND] = maxY
            coordBox[DOWN] = minZ
            coordBox[UP] = maxZ
        }

        companion object {
            internal val LEFT = 0
            internal val RIGHT = 1
            internal val FRONT = 2
            internal val BEHIND = 3
            internal val DOWN = 4
            internal val UP = 5
        }
    }

    class Vector(val x: Float, val y: Float, val z: Float) {
        companion object {

            fun substract(v1: Vector, v2: Vector): Vector {
                val x = v1.x - v2.x
                val y = v1.y - v2.y
                val z = v1.z - v2.z

                return Vector(x, y, z)

            }

            fun crossProduct(v: Vector, v2: Vector): Vector {
                val x = v.y * v2.z - v.z * v2.y
                val y = v.z * v2.x - v.x * v2.z
                val z = v.x * v2.y - v.y * v2.x

                return Vector(x, y, z)
            }

            fun normalize(v: Vector): Vector {
                val length = Math.sqrt((v.x * v.x + v.y * v.y + v.z * v.z).toDouble()).toFloat()
                return Vector(v.x / length, v.y / length, v.z / length)
            }
        }
    }

    class Ray(val point: Point, val vector: Vector)

    fun intersects(box: Box, ray: Ray): Boolean {
        var index = 0
        var k: Float
        var x = java.lang.Float.MIN_VALUE
        var y = java.lang.Float.MIN_VALUE
        var z = java.lang.Float.MIN_VALUE

        while (index < box.coordBox.size) {
            when (index) {
                Box.LEFT, Box.RIGHT -> {
                    k = (box.coordBox[index] - ray.point.x) / ray.vector.x
                    x = box.coordBox[index]
                    y = ray.point.y + k * ray.vector.y
                    z = ray.point.z + k * ray.vector.z
                }
                Box.BEHIND, Box.FRONT -> {
                    k = (box.coordBox[index] - ray.point.y) / ray.vector.y
                    x = ray.point.x + k * ray.vector.x
                    y = box.coordBox[index]
                    z = ray.point.z + k * ray.vector.z
                }
                Box.UP, Box.DOWN -> {
                    k = (box.coordBox[index] - ray.point.z) / ray.vector.z
                    x = ray.point.x + k * ray.vector.x
                    y = ray.point.y + k * ray.vector.y
                    z = box.coordBox[index]
                }
            }

            //Check if (x,y,z) is a box point
            if (x >= box.coordBox[Box.LEFT] && x <= box.coordBox[Box.RIGHT] &&
                y >= box.coordBox[Box.FRONT] && y <= box.coordBox[Box.BEHIND] &&
                z >= box.coordBox[Box.DOWN] && z <= box.coordBox[Box.UP]
            )
                return true

            index++
        }

        return false
    }

    fun vectorBetween(from: Point, to: Point): Vector {
        return Vector(
            to.x - from.x,
            to.y - from.y,
            to.z - from.z
        )
    }

    fun intersectionPointWitboxPlate(ray: Ray): Point {
        //plane is z=centerZ
        val k = (0 - ray.point.z) / ray.vector.z
        val x = ray.point.x + k * ray.vector.x
        val y = ray.point.y + k * ray.vector.y
        val z = 0f

        return Point(x, y, z)
    }

    fun overlaps(maxX: Float, minX: Float, maxY: Float, minY: Float, d: DataStorage): Boolean {
        val maxX2 = d.maxX
        val maxY2 = d.maxY

        val minX2 = d.minX
        val minY2 = d.minY

        if ((maxX >= minX2 && maxX <= maxX2 || minX <= maxX2 && minX >= minX2 || minX >= minX2 && maxX <= maxX2) && (maxY >= minY2 && maxY <= maxY2 || minY <= maxY2 && minY >= minY2 || minY >= minY2 && maxY <= maxY2)) {
            return true
        }

        if ((maxX2 >= minX && maxX2 <= maxX || minX2 <= maxX && minX2 >= minX || minX2 >= minX && maxX2 <= maxX) && (maxY2 >= minY && maxY2 <= maxY || minY2 <= maxY && minY2 >= minY || minY2 >= minY && maxY2 <= maxY)) {
            return true
        }

        //New cases that were not being considered
        return if (minX >= minX2 && maxX <= maxX2 && maxY >= maxY2 && minY <= minY2 || minX2 >= minX && maxX2 <= maxX && maxY2 >= maxY && minY2 <= minY) {
            true
        } else false

    }

    fun relocateIfOverlaps(objects: List<DataStorage>): Boolean {
        val objectToFit = objects.size - 1

        val data: DataStorage

        //TODO random crash
        try {

            data = objects[objectToFit]

        } catch (e: ArrayIndexOutOfBoundsException) {

            e.printStackTrace()
            return false
        }

        var overlaps = false

        for (i in objects.indices) {
            if (i != objectToFit && Geometry.overlaps(data.maxX, data.minX, data.maxY, data.minY, objects[i])) {
                overlaps = true
                break
            }
        }

        if (!overlaps) return false

        val width = data.maxX - data.minX
        val deep = data.maxY - data.minY

        var setMinX = java.lang.Float.MAX_VALUE
        var index = -1

        var newMaxX: Float
        var newMinX: Float
        var newMaxY: Float
        var newMinY: Float

        for (i in objects.indices) {
            if (i != objectToFit) {
                val d = objects[i]
                if (d.minX < setMinX) {
                    setMinX = d.minX
                    index = i
                }
                //UP
                newMaxX = d.maxX
                newMinX = d.minX
                newMaxY = d.lastCenter.y + Math.abs(d.maxY - d.lastCenter.y) + deep + OFFSET
                newMinY = d.lastCenter.y + Math.abs(d.maxY - d.lastCenter.y) + OFFSET

                if (isValidPosition(newMaxX, newMinX, newMaxY, newMinY, objects, objectToFit)) {
                    changeModelToFit(newMaxX, newMinX, newMaxY, newMinY, data)
                    break
                }

                //RIGHT
                newMaxX = d.lastCenter.x + Math.abs(d.maxX - d.lastCenter.x) + width + OFFSET
                newMinX = d.lastCenter.x + Math.abs(d.maxX - d.lastCenter.x) + OFFSET
                newMaxY = d.maxY
                newMinY = d.minY

                if (isValidPosition(newMaxX, newMinX, newMaxY, newMinY, objects, objectToFit)) {
                    changeModelToFit(newMaxX, newMinX, newMaxY, newMinY, data)
                    break
                }

                //DOWN
                newMaxX = d.maxX
                newMinX = d.minX
                newMaxY = d.lastCenter.y - (Math.abs(d.minY - d.lastCenter.y) + OFFSET)
                newMinY = d.lastCenter.y - (Math.abs(d.minY - d.lastCenter.y) + deep + OFFSET)

                if (isValidPosition(newMaxX, newMinX, newMaxY, newMinY, objects, objectToFit)) {
                    changeModelToFit(newMaxX, newMinX, newMaxY, newMinY, data)
                    break
                }

                //LEFT
                newMaxX = d.lastCenter.x - (Math.abs(d.minX - d.lastCenter.x) + OFFSET)
                newMinX = d.lastCenter.x - (Math.abs(d.minX - d.lastCenter.x) + width + OFFSET)
                newMaxY = d.maxY
                newMinY = d.minY

                if (isValidPosition(newMaxX, newMinX, newMaxY, newMinY, objects, objectToFit)) {
                    changeModelToFit(newMaxX, newMinX, newMaxY, newMinY, data)
                    break
                } else if (i == objects.size - 2) {

                    return false

                    /*newMaxX = setMinX - OFFSET;
					newMinX = setMinX - (width + OFFSET);
					newMaxY = objects.get(index).getMaxY()+OFFSET;
					newMinY = objects.get(index).getMinY()+OFFSET;

					data.setStateObject(ViewerRenderer.OUT_NOT_TOUCHED);


					changeModelToFit(newMaxX, newMinX, newMaxY, newMinY, data);*/
                }
            }
        }

        return true
    }

    fun isValidPosition(
        newMaxX: Float,
        newMinX: Float,
        newMaxY: Float,
        newMinY: Float,
        objects: List<DataStorage>,
        `object`: Int
    ): Boolean {
        var overlaps = false
        var outOfPlate = false
        var k = 0

        val auxPlate = ViewerMainFragment.currentPlate

        if (newMaxX > auxPlate[0] || newMinX < -auxPlate[0]
            || newMaxY > auxPlate[1] || newMinY < -auxPlate[1]
        )
            outOfPlate = true

        while (!outOfPlate && !overlaps && k < objects.size) {
            if (k != `object`) {
                if (Geometry.overlaps(newMaxX, newMinX, newMaxY, newMinY, objects[k])) overlaps = true
            }
            k++
        }

        return if (!outOfPlate && !overlaps)
            true
        else
            false
    }

    fun changeModelToFit(newMaxX: Float, newMinX: Float, newMaxY: Float, newMinY: Float, d: DataStorage) {
        d.maxX = newMaxX
        d.minX = newMinX
        d.maxY = newMaxY
        d.minY = newMinY

        val newCenterX = newMinX + (newMaxX - newMinX) / 2
        val newCenterY = newMinY + (newMaxY - newMinY) / 2
        val newCenterZ = d.lastCenter.z

        val newCenter = Point(newCenterX, newCenterY, newCenterZ)

        d.lastCenter = newCenter

        val temporaryModel = FloatArray(16)
        Matrix.setIdentityM(temporaryModel, 0)
        Matrix.translateM(temporaryModel, 0, d.lastCenter.x, d.lastCenter.y, d.lastCenter.z)
        Matrix.scaleM(temporaryModel, 0, d.lastScaleFactorX, d.lastScaleFactorY, d.lastScaleFactorZ)

        Matrix.translateM(temporaryModel, 0, 0f, 0f, d.adjustZ)

        //Object rotation
        val rotateObjectMatrix = d.rotationMatrix

        //Multiply the model by the accumulated rotation
        val modelMatrix = FloatArray(16)

        Matrix.multiplyMM(modelMatrix, 0, temporaryModel, 0, rotateObjectMatrix, 0)
        d.modelMatrix = modelMatrix
    }
}
