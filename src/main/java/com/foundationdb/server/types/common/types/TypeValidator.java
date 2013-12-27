/**
 * Copyright (C) 2009-2013 FoundationDB, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.server.types.common.types;

import com.foundationdb.server.types.TClass;
import com.foundationdb.server.types.TInstance;

import java.sql.Types;

public class TypeValidator
{
    private TypeValidator() {
    }

    // TODO: Some of these are properly constraints on the
    // store. Revisit when RowData is no longer the default.

    public static boolean isSupportedForColumn(TInstance tinstance) {
        TClass tclass = TInstance.tClass(tinstance);
        return ((tclass != null) && isSupportedForColumn(tclass));
    }

    public static boolean isSupportedForColumn(TClass type) {
        return (type.jdbcType() != Types.OTHER);
    }

    public static boolean isSupportedForIndex(TInstance tinstance) {
        TClass tclass = TInstance.tClass(tinstance);
        return ((tclass != null) && isSupportedForIndex(tclass));
    }

    public static boolean isSupportedForIndex(TClass type) {
        switch (type.jdbcType()) {
        case Types.BLOB:
        case Types.CLOB:
        case Types.LONGVARBINARY:
        case Types.LONGVARCHAR:
        case Types.LONGNVARCHAR:
            return false;
        default:
            return true;
        }
    }

    public static boolean isSupportedForJoin(TInstance tinstance1, TInstance tinstance2) {
        TClass tclass1 = TInstance.tClass(tinstance1);
        TClass tclass2 = TInstance.tClass(tinstance2);
        return ((tclass1 != null) && (tclass2 != null) &&
                isSupportedForJoin(tclass1, tclass2));
    }
    
    public static boolean isSupportedForJoin(TClass tclass1, TClass tclass2) {
        if (tclass1 == tclass2) {
            return true;
        }
        int jt1 = baseJoinType(tclass1);
        int jt2 = baseJoinType(tclass2);
        if (jt1 == jt2) {
            switch (jt1) {
            case Types.DISTINCT:
            case Types.JAVA_OBJECT:
            case Types.OTHER:
            case Types.STRUCT:
                return false;
            default:
                return true;
            }
        }
        else {
            return false;
        }
    }

    // Want to allow: CHAR & VARCHAR, INT & BIGINT, INT & INT UNSIGNED, etc.
    // TODO: Also allows DATETIME & TIMESTAMP, and even cross-bundle;
    // will that be okay?
    protected static int baseJoinType(TClass tclass) {
        int jdbcType = tclass.jdbcType();
        switch (jdbcType) {
        case Types.BIGINT:
            if (tclass.isUnsigned())
                return Types.OTHER;
        /* else falls through */
        case Types.TINYINT:
        case Types.INTEGER:
        case Types.SMALLINT:
            return Types.BIGINT;
        case Types.NUMERIC:
        case Types.DECIMAL:
            return Types.DECIMAL;
        case Types.FLOAT:
        case Types.REAL:
        case Types.DOUBLE:
            return Types.DOUBLE;
        case Types.CHAR:
        case Types.NCHAR:
        case Types.NVARCHAR:
        case Types.VARCHAR:
            return Types.VARCHAR;
        case Types.LONGNVARCHAR:
        case Types.LONGVARCHAR:
        case Types.CLOB:
            return Types.LONGVARCHAR;
        case Types.BINARY:
        case Types.BIT:
        case Types.VARBINARY:
            return Types.VARBINARY;
        case Types.LONGVARBINARY:
        case Types.BLOB:
            return Types.LONGVARBINARY;
        default:
            return jdbcType;
        }
    }

}