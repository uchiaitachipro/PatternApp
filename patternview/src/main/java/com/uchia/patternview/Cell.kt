package com.uchia.patternview

import android.os.Parcel
import android.os.Parcelable
import com.uchia.patternview.extensions.CellUtils

class Cell(): Parcelable{

    var clickEvent  : ClickListener? = null
    var row = 0
    var column = 0
    var offsetX = 0
    var offsetY = 0
    var dontHit = false
    var isSelected = false
    constructor(
            row : Int,
            column : Int,
            offsetX : Int = 0,
            offsetY : Int = 0,
            dontHit : Boolean = false,
            isSelected : Boolean = false):this(){
        CellUtils.checkRange(row, column)
        this.row = row
        this.column = column
        this.offsetX = offsetX
        this.offsetY = offsetY
        this.dontHit = dontHit
        this.isSelected = isSelected
    }

    fun getId(): String {
        val formatRow = String.format("%03d", row)
        val formatColumn = String.format("%03d", column)
        return "$formatRow-$formatColumn"
    }

    override fun toString(): String {
        return "(r= $row  c= $column)"
    }


    override fun equals(obj: Any?): Boolean {
        return if (obj is Cell) {
            this.column == obj.column && this.row == obj.row
        } else super.equals(obj)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(row)
        parcel.writeInt(column)
        parcel.writeInt(offsetX)
        parcel.writeInt(offsetY)
        parcel.writeByte(if (dontHit) 1 else 0)
        parcel.writeByte(if (isSelected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    constructor(parcel: Parcel) : this() {
        row = parcel.readInt()
        column = parcel.readInt()
        offsetX = parcel.readInt()
        offsetY = parcel.readInt()
        dontHit = parcel.readByte() != 0.toByte()
        isSelected = parcel.readByte() != 0.toByte()
    }

    companion object CREATOR : Parcelable.Creator<Cell> {
        override fun createFromParcel(parcel: Parcel): Cell {
            return Cell(parcel)
        }

        override fun newArray(size: Int): Array<Cell?> {
            return arrayOfNulls(size)
        }
    }


    interface ClickListener{
        fun click(cell : Cell)
    }

}