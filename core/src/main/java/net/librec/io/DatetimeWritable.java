/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@SuppressWarnings("rawtypes")
public class DatetimeWritable implements WritableComparable {

    /** the value of the DatetimeWritable */
    private long value;

    /**
     * Empty constructor
     */
    public DatetimeWritable() {
    }

    /**
     * Create a new object with the given value.
     *
     * @param value a given value
     */
    public DatetimeWritable(long value) {
        set(value);
    }

    /**
     * Set the value of DatetimeWritable.
     *
     * @param value the value of DatetimeWritable
     */
    public void set(long value) {
        this.value = value;
    }

    /**
     * Get the value of DatetimeWritable.
     *
     * @return  the value of DatetimeWritable
     */
    public long get() {
        return value;
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        value = in.readLong();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DatetimeWritable)) {
            return false;
        }
        DatetimeWritable other = (DatetimeWritable) o;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return (int) value;
    }

    @Override
    public int compareTo(Object o) {
        long thisValue = this.value;
        long thatValue = ((DatetimeWritable) o).value;
        return (thisValue < thatValue ? -1 : (thisValue == thatValue ? 0 : 1));
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }


    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value = (long) value;
    }

}
