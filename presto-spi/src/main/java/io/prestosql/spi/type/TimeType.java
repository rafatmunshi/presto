/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.spi.type;

import io.airlift.slice.XxHash64;
import io.prestosql.spi.PrestoException;
import io.prestosql.spi.block.Block;
import io.prestosql.spi.connector.ConnectorSession;
import io.prestosql.spi.function.ScalarOperator;

import static io.prestosql.spi.StandardErrorCode.NUMERIC_VALUE_OUT_OF_RANGE;
import static io.prestosql.spi.function.OperatorType.COMPARISON;
import static io.prestosql.spi.function.OperatorType.EQUAL;
import static io.prestosql.spi.function.OperatorType.HASH_CODE;
import static io.prestosql.spi.function.OperatorType.LESS_THAN;
import static io.prestosql.spi.function.OperatorType.LESS_THAN_OR_EQUAL;
import static io.prestosql.spi.function.OperatorType.XX_HASH_64;
import static io.prestosql.spi.type.TypeOperatorDeclaration.extractOperatorDeclaration;
import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;

/**
 * A time is stored as picoseconds from midnight.
 */
public final class TimeType
        extends AbstractLongType
{
    private static final TypeOperatorDeclaration TYPE_OPERATOR_DECLARATION = extractOperatorDeclaration(TimeType.class, lookup(), long.class);

    public static final int MAX_PRECISION = 12;
    public static final int DEFAULT_PRECISION = 3; // TODO: should be 6 per SQL spec

    private static final TimeType[] TYPES = new TimeType[MAX_PRECISION + 1];

    static {
        for (int precision = 0; precision <= MAX_PRECISION; precision++) {
            TYPES[precision] = new TimeType(precision);
        }
    }

    public static final TimeType TIME_SECONDS = createTimeType(0);
    public static final TimeType TIME_MILLIS = createTimeType(3);
    public static final TimeType TIME_MICROS = createTimeType(6);
    public static final TimeType TIME_NANOS = createTimeType(9);
    public static final TimeType TIME_PICOS = createTimeType(12);

    /**
     * @deprecated Use {@link #TIME_MILLIS} instead
     */
    @Deprecated
    public static final TimeType TIME = new TimeType(DEFAULT_PRECISION);

    private final int precision;

    private TimeType(int precision)
    {
        super(new TypeSignature(StandardTypes.TIME, TypeSignatureParameter.numericParameter(precision)));
        this.precision = precision;
    }

    public static TimeType createTimeType(int precision)
    {
        if (precision < 0 || precision > MAX_PRECISION) {
            throw new PrestoException(NUMERIC_VALUE_OUT_OF_RANGE, format("TIME precision must be in range [0, %s]: %s", MAX_PRECISION, precision));
        }
        return TYPES[precision];
    }

    public int getPrecision()
    {
        return precision;
    }

    @Override
    public TypeOperatorDeclaration getTypeOperatorDeclaration(TypeOperators typeOperators)
    {
        return TYPE_OPERATOR_DECLARATION;
    }

    @Override
    public Object getObjectValue(ConnectorSession session, Block block, int position)
    {
        if (block.isNull(position)) {
            return null;
        }

        return SqlTime.newInstance(precision, block.getLong(position, 0));
    }

    @ScalarOperator(EQUAL)
    public static boolean equalOperator(long left, long right)
    {
        return left == right;
    }

    @ScalarOperator(HASH_CODE)
    public static long hashCodeOperator(long value)
    {
        return AbstractLongType.hash(value);
    }

    @ScalarOperator(XX_HASH_64)
    public static long xxHash64Operator(long value)
    {
        return XxHash64.hash(value);
    }

    @ScalarOperator(COMPARISON)
    private static long comparisonOperator(long left, long right)
    {
        return Long.compare(left, right);
    }

    @ScalarOperator(LESS_THAN)
    public static boolean lessThan(long left, long right)
    {
        return left < right;
    }

    @ScalarOperator(LESS_THAN_OR_EQUAL)
    public static boolean lessThanOrEqual(long left, long right)
    {
        return left <= right;
    }
}
