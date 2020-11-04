/*
 * Copyright 2019-2020 The Polypheny Project
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
 */

package org.polypheny.db.adapter.cottontail.util;


import java.util.List;
import org.polypheny.db.adapter.cottontail.enumberable.CottontailQueryEnumerable;
import org.vitrivr.cottontail.grpc.CottontailGrpc;
import org.vitrivr.cottontail.grpc.CottontailGrpc.AtomicLiteralBooleanPredicate;
import org.vitrivr.cottontail.grpc.CottontailGrpc.CompoundBooleanPredicate;
import org.vitrivr.cottontail.grpc.CottontailGrpc.Data;
import org.vitrivr.cottontail.grpc.CottontailGrpc.Knn;
import org.vitrivr.cottontail.grpc.CottontailGrpc.Knn.Distance;
import org.vitrivr.cottontail.grpc.CottontailGrpc.Vector;
import org.vitrivr.cottontail.grpc.CottontailGrpc.Where;


public class Linq4JFixer {


    public static Object getBooleanData( Object data ) {
        return ((CottontailGrpc.Data) data).getBooleanData();
    }


    public static Object getIntData( Object data ) {
        return ((CottontailGrpc.Data) data).getIntData();
    }


    public static Object getLongData( Object data ) {
        return ((CottontailGrpc.Data) data).getLongData();
    }


    public static Object getFloatData( Object data ) {
        return ((CottontailGrpc.Data) data).getFloatData();
    }


    public static Object getDoubleData( Object data ) {
        return ((CottontailGrpc.Data) data).getDoubleData();
    }


    public static Object getStringData( Object data ) {
        return ((CottontailGrpc.Data) data).getStringData();
    }


    public static Object getNullData( Object data ) {
        return ((CottontailGrpc.Data) data).getNullData();
    }


    public static Object getBoolVector( Object data ) {
        return ((CottontailGrpc.Data) data).getVectorData().getBoolVector().getVectorList();
    }


    public static Object getIntVector( Object data ) {
        return ((CottontailGrpc.Data) data).getVectorData().getIntVector().getVectorList();
    }

    public static Object getFloatVector( Object data ) {
        return ((CottontailGrpc.Data) data).getVectorData().getFloatVector().getVectorList();
    }

    public static Object getDoubleVector( Object data ) {
        return ((CottontailGrpc.Data) data).getVectorData().getDoubleVector().getVectorList();
    }

    public static Object getLongVector( Object data ) {
        return ((CottontailGrpc.Data) data).getVectorData().getLongVector().getVectorList();
    }


    public static CompoundBooleanPredicate generateCompoundPredicate(
            Object operator_,
//                CompoundBooleanPredicate.Operator operator,
            Object left,
            Object right
    ) {
        CompoundBooleanPredicate.Operator operator = (CompoundBooleanPredicate.Operator) operator_;
        CompoundBooleanPredicate.Builder builder = CompoundBooleanPredicate.newBuilder();
        builder = builder.setOp( operator );

        if ( left instanceof AtomicLiteralBooleanPredicate ) {
            builder = builder.setAleft( (AtomicLiteralBooleanPredicate) left );
        } else {
            builder = builder.setCleft( (CompoundBooleanPredicate) left );
        }

        if ( right instanceof AtomicLiteralBooleanPredicate ) {
            builder = builder.setAright( (AtomicLiteralBooleanPredicate) right );
        } else {
            builder = builder.setCright( (CompoundBooleanPredicate) right );
        }

        return builder.build();
    }


    public static AtomicLiteralBooleanPredicate generateAtomicPredicate(
            String attribute,
            Boolean not,
            Object operator_,
            Object data_
    ) {
        AtomicLiteralBooleanPredicate.Operator operator = (AtomicLiteralBooleanPredicate.Operator) operator_;
        Data data = (Data) data_;
        return AtomicLiteralBooleanPredicate.newBuilder()
                .addData( data )
                .setNot( not )
                .setAttribute( attribute )
                .setOp( operator )
                .build();
    }


    public static Where generateWhere( Object filterExpression ) {
        if ( filterExpression instanceof AtomicLiteralBooleanPredicate ) {
            return Where.newBuilder().setAtomic( (AtomicLiteralBooleanPredicate) filterExpression ).build();
        }

        if ( filterExpression instanceof CompoundBooleanPredicate ) {
            return Where.newBuilder().setCompound( (CompoundBooleanPredicate) filterExpression ).build();
        }

        throw new RuntimeException( "Not a proper filter expression!" );
    }


    /*public static Knn generateKnn(
            String column,
            String distance,
            Object target,
            Object fourthArgument
    ) {

    }*/


    public static Knn generateKnn(
            Object column,
            Object k,
            Object distance,
            Object target,
            Object weights ) {
        Knn.Builder knnBuilder = Knn.newBuilder();

        knnBuilder.setAttribute( (String) column );

        if ( k != null ) {
            knnBuilder.setK( (Integer) k );
        } else {
            // Cottontail requires a k to be set for these queries.
            // Setting Integer.MAX_VALUE will cause an out of memory with cottontail
            // 2050000000 still works, 2100000000 doesn't work
            // I will just decide that 1000000 is a reasonable default value!
            knnBuilder.setK( 1000000 );
        }

        if ( target != null ) {
            knnBuilder.addQuery( (Vector) target );
//            knnBuilder.setQuery( 0, (Vector) target );
        }

        if ( weights != null ) {
            knnBuilder.addWeights( (Vector) weights );
//            knnBuilder.setWeights( 0, (Vector) weights );
        }

        knnBuilder.setDistance( getDistance( (String) distance ) );

        return knnBuilder.build();
    }


    public static Knn.Distance getDistance( String norm ) {
        switch ( norm ) {
            case "L1":
                return Distance.L1;
            case "L2":
                return Distance.L2;
            case "L2SQUARED":
                return Distance.L2SQUARED;
            case "CHISQUARED":
                return Distance.CHISQUARED;
            case "COSINE":
                return Distance.COSINE;
            default:
                throw new IllegalArgumentException( "Unknown norm: " + norm );
        }
    }

}
