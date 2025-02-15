/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.compute.data;

$if(BytesRef)$
import org.apache.lucene.util.BytesRef;
$endif$
import org.apache.lucene.util.RamUsageEstimator;

/**
 * Vector implementation that stores a constant $type$ value.
 * This class is generated. Do not edit it.
 */
public final class Constant$Type$Vector extends AbstractVector implements $Type$Vector {

$if(BytesRef)$
    static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(ConstantBytesRefVector.class) + RamUsageEstimator
        .shallowSizeOfInstance(BytesRef.class);
$else$
    static final long RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(Constant$Type$Vector.class);
$endif$

    private final $type$ value;

    public Constant$Type$Vector($type$ value, int positionCount) {
        this(value, positionCount, BlockFactory.getNonBreakingInstance());
    }

    public Constant$Type$Vector($type$ value, int positionCount, BlockFactory blockFactory) {
        super(positionCount, blockFactory);
        this.value = value;
    }

    @Override
$if(BytesRef)$
    public BytesRef getBytesRef(int position, BytesRef ignore) {
$else$
    public $type$ get$Type$(int position) {
$endif$
        return value;
    }

    @Override
    public $Type$Block asBlock() {
        return new $Type$VectorBlock(this);
    }

    @Override
    public $Type$Vector filter(int... positions) {
        return new Constant$Type$Vector(value, positions.length);
    }

    @Override
    public ElementType elementType() {
        return ElementType.$TYPE$;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public long ramBytesUsed() {
$if(BytesRef)$
        return BASE_RAM_BYTES_USED + RamUsageEstimator.sizeOf(value.bytes);
$else$
        return RAM_BYTES_USED;
$endif$
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof $Type$Vector that) {
            return $Type$Vector.equals(this, that);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return $Type$Vector.hash(this);
    }

    public String toString() {
        return getClass().getSimpleName() + "[positions=" + getPositionCount() + ", value=" + value + ']';
    }

    @Override
    public void close() {
        blockFactory.adjustBreaker(-ramBytesUsed(), true);
    }
}
