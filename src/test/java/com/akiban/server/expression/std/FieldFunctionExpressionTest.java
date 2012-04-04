/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */

package com.akiban.server.expression.std;

import com.akiban.junit.OnlyIf;
import com.akiban.server.error.WrongExpressionArityException;
import org.junit.Test;
import com.akiban.junit.OnlyIfNot;
import com.akiban.junit.NamedParameterizedRunner;
import com.akiban.junit.NamedParameterizedRunner.TestParameters;
import com.akiban.junit.Parameterization;
import com.akiban.junit.ParameterizationBuilder;
import com.akiban.server.expression.Expression;
import com.akiban.server.types.AkType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static com.akiban.server.expression.std.ExprUtil.*;
import static com.akiban.server.types.AkType.*;

@RunWith(NamedParameterizedRunner.class)
public class FieldFunctionExpressionTest 
{
    private List<? extends Expression> args;
    private Long expected;
    
    public FieldFunctionExpressionTest (List<? extends Expression> args, Long expected)
    {
        this.args = args;
        this.expected = expected;
    }
    
    @TestParameters
    public static Collection<Parameterization> params()
    {
        ParameterizationBuilder pb = new ParameterizationBuilder();
        
        param(pb, 0L, lit(2L), lit(1L));
        return pb.asList();
    }
    
    private static void param(ParameterizationBuilder pb, Long exp, Expression...args)
    {
        List<? extends Expression> argsList = Arrays.asList(args);
        pb.add("FIELD(", args +") expcted: " + (exp == null ? "NULL" : exp),
                exp,
                argsList);
    }
    
    @OnlyIfNot("expectArityException()")
    @Test
    public void test()
    {
        doTest();
    }
    
    @OnlyIf("expectArityException()")
    @Test(expected=WrongExpressionArityException.class)
    public void testArity()
    {
        doTest();
    }
    
    private void doTest()
    {
        Expression top = new FieldFunctionExcpression(args);
        assertEquals(expected.longValue(), top.evaluation().eval().getLong());
    }
    
    public boolean expectArityException ()
    {
        return expected == null;
    }
}
