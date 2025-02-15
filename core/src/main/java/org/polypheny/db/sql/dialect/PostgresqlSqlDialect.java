/*
 * Copyright 2019-2021 The Polypheny Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file incorporates code covered by the following terms:
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polypheny.db.sql.dialect;


import org.apache.calcite.avatica.SqlType;
import org.apache.calcite.avatica.util.TimeUnitRange;
import org.polypheny.db.rel.type.RelDataType;
import org.polypheny.db.rel.type.RelDataTypeSystem;
import org.polypheny.db.rel.type.RelDataTypeSystemImpl;
import org.polypheny.db.sql.SqlCall;
import org.polypheny.db.sql.SqlDataTypeSpec;
import org.polypheny.db.sql.SqlDialect;
import org.polypheny.db.sql.SqlIdentifier;
import org.polypheny.db.sql.SqlLiteral;
import org.polypheny.db.sql.SqlNode;
import org.polypheny.db.sql.SqlWriter;
import org.polypheny.db.sql.fun.SqlFloorFunction;
import org.polypheny.db.sql.parser.SqlParserPos;
import org.polypheny.db.type.PolyType;


/**
 * A <code>SqlDialect</code> implementation for the PostgreSQL database.
 */
public class PostgresqlSqlDialect extends SqlDialect {

    /**
     * PostgreSQL type system.
     */
    private static final RelDataTypeSystem POSTGRESQL_TYPE_SYSTEM =
            new RelDataTypeSystemImpl() {
                @Override
                public int getMaxPrecision( PolyType typeName ) {
                    switch ( typeName ) {
                        case VARCHAR:
                            // From htup_details.h in postgresql:
                            // MaxAttrSize is a somewhat arbitrary upper limit on the declared size of data fields of char(n) and similar types.  It need not have anything
                            // directly to do with the *actual* upper limit of varlena values, which is currently 1Gb (see TOAST structures in postgres.h).  I've set it
                            // at 10Mb which seems like a reasonable number --- tgl 8/6/00. */
                            return 10 * 1024 * 1024;
                        default:
                            return super.getMaxPrecision( typeName );
                    }
                }
            };

    public static final SqlDialect DEFAULT =
            new PostgresqlSqlDialect( EMPTY_CONTEXT
                    .withDatabaseProduct( DatabaseProduct.POSTGRESQL )
                    .withIdentifierQuoteString( "\"" )
                    .withDataTypeSystem( POSTGRESQL_TYPE_SYSTEM ) );


    /**
     * Creates a PostgresqlSqlDialect.
     */
    public PostgresqlSqlDialect( Context context ) {
        super( context );
    }


    @Override
    public boolean supportsCharSet() {
        return false;
    }


    @Override
    public IntervalParameterStrategy getIntervalParameterStrategy() {
        return IntervalParameterStrategy.CAST;
    }


    @Override
    public boolean supportsNestedArrays() {
        return true;
    }


    @Override
    public SqlNode getCastSpec( RelDataType type ) {
        String castSpec;
        switch ( type.getPolyType() ) {
            case TINYINT:
                // Postgres has no tinyint (1 byte), so instead cast to smallint (2 bytes)
                castSpec = "_smallint";
                break;
            case DOUBLE:
                // Postgres has a double type but it is named differently
                castSpec = "_double precision";
                break;
            case FILE:
            case IMAGE:
            case VIDEO:
            case SOUND:
                castSpec = "_BYTEA";
                break;
            case ARRAY:
                RelDataType tt = type;
                StringBuilder brackets = new StringBuilder( "[]" );
                while ( tt.getComponentType().getPolyType() == PolyType.ARRAY ) {
                    tt = tt.getComponentType();
                    brackets.append( "[]" );
                }
                PolyType t = tt.getComponentType().getPolyType();
                switch ( t ) {
                    case TINYINT:
                        castSpec = "_smallint" + brackets;
                        break;
                    case DOUBLE:
                        castSpec = "_double precision" + brackets;
                        break;
                    default:
                        castSpec = "_" + t.getName() + brackets;
                }
                break;
            case INTERVAL_YEAR_MONTH:
            case INTERVAL_DAY:
            case INTERVAL_DAY_HOUR:
            case INTERVAL_DAY_MINUTE:
            case INTERVAL_DAY_SECOND:
            case INTERVAL_HOUR_MINUTE:
            case INTERVAL_HOUR:
            case INTERVAL_HOUR_SECOND:
            case INTERVAL_MINUTE:
            case INTERVAL_MONTH:
            case INTERVAL_SECOND:
            case INTERVAL_MINUTE_SECOND:
            case INTERVAL_YEAR:
                castSpec = "interval";
                break;
            default:
                return super.getCastSpec( type );
        }

        return new SqlDataTypeSpec( new SqlIdentifier( castSpec, SqlParserPos.ZERO ), -1, -1, null, null, SqlParserPos.ZERO );
    }


    @Override
    public String getArrayComponentTypeString( SqlType type ) {
        switch ( type ) {
            case TINYINT:
                return "int2"; // Postgres has no tinyint (1 byte), so instead cast to smallint (2 bytes)
            case DOUBLE:
                return "float8";
            default:
                return super.getArrayComponentTypeString( type );
        }
    }


    @Override
    protected boolean requiresAliasForFromItems() {
        return true;
    }


    @Override
    public boolean supportsNestedAggregations() {
        return false;
    }


    @Override
    public void unparseCall( SqlWriter writer, SqlCall call, int leftPrec, int rightPrec ) {
        switch ( call.getKind() ) {
            case FLOOR:
                if ( call.operandCount() != 2 ) {
                    super.unparseCall( writer, call, leftPrec, rightPrec );
                    return;
                }

                final SqlLiteral timeUnitNode = call.operand( 1 );
                final TimeUnitRange timeUnit = timeUnitNode.getValueAs( TimeUnitRange.class );

                SqlCall call2 = SqlFloorFunction.replaceTimeUnitOperand( call, timeUnit.name(), timeUnitNode.getParserPosition() );
                SqlFloorFunction.unparseDatetimeFunction( writer, call2, "DATE_TRUNC", false );
                break;

            default:
                super.unparseCall( writer, call, leftPrec, rightPrec );
        }
    }

}

