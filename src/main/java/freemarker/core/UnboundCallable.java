/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
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

package freemarker.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import freemarker.template.TemplateModel;

/**
 * Represents the definition of a macro or function (or other future callable entity) in the AST. For understanding
 * related concepts more, see {@link BoundCallable}.
 * 
 * <p>
 * Historical note: Prior to 2.4, the two concepts ({@link UnboundCallable} and {@link BoundCallable}) were these same,
 * represented by {@link Macro}, which still exists due to backward compatibility constraints, but now is abstract and
 * is implemented by this class. This class should not implement {@link TemplateModel} (which it does, because
 * {@link Macro} implements it), but it had to, for backward compatibility.
 * 
 * @see BoundCallable
 * 
 * @since 2.4.0
 */
class UnboundCallable extends Macro {

    static final UnboundCallable NO_OP_MACRO = new UnboundCallable(".pass", 
            Collections.EMPTY_LIST, 
            Collections.EMPTY_MAP,
            null, false,
            TextBlock.EMPTY_BLOCK);
    
    final static int TYPE_MACRO = 0;
    final static int TYPE_FUNCTION = 1;
    
    private final String name;
    private final String[] paramNames;
    private final Map paramDefaults;
    private final String catchAllParamName;
    private final boolean function;

    UnboundCallable(String name, List argumentNames, Map args, 
            String catchAllParamName, boolean function,
            TemplateElement nestedBlock) 
    {
        this.name = name;
        this.paramNames = (String[])argumentNames.toArray(
                new String[argumentNames.size()]);
        this.paramDefaults = args;
        
        this.function = function;
        this.catchAllParamName = catchAllParamName; 
        
        this.nestedBlock = nestedBlock;
    }
    
    String[] getParamNames() {
        return paramNames;
    }
    
    Map getParamDefaults() {
        return paramDefaults;
    }

    public String getCatchAll() {
        return catchAllParamName;
    }
    
    public String[] getArgumentNames() {
        return paramNames.clone();
    }

    String[] getArgumentNamesInternal() {
        return paramNames;
    }

    boolean hasArgNamed(String name) {
        return paramDefaults.containsKey(name);
    }
    
    public String getName() {
        return name;
    }

    void accept(Environment env) {
        env.visitCallableDefinition(this);
    }

    protected String dump(boolean canonical) {
        StringBuffer sb = new StringBuffer();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        sb.append(' ');
        sb.append(_CoreStringUtils.toFTLTopLevelTragetIdentifier(name));
        if (function) sb.append('(');
        int argCnt = paramNames.length;
        for (int i = 0; i < argCnt; i++) {
            if (function) {
                if (i != 0) {
                    sb.append(", ");
                }
            } else {
                sb.append(' ');
            }
            String argName = paramNames[i];
            sb.append(_CoreStringUtils.toFTLTopLevelIdentifierReference(argName));
            if (paramDefaults != null && paramDefaults.get(argName) != null) {
                sb.append('=');
                Expression defaultExpr = (Expression) paramDefaults.get(argName);
                if (function) {
                    sb.append(defaultExpr.getCanonicalForm());
                } else {
                    MessageUtil.appendExpressionAsUntearable(sb, defaultExpr);
                }
            }
        }
        if (catchAllParamName != null) {
            if (function) {
                if (argCnt != 0) {
                    sb.append(", ");
                }
            } else {
                sb.append(' ');
            }
            sb.append(catchAllParamName);
            sb.append("...");
        }
        if (function) sb.append(')');
        if (canonical) {
            sb.append('>');
            if (nestedBlock != null) {
                sb.append(nestedBlock.getCanonicalForm());
            }
            sb.append("</").append(getNodeTypeSymbol()).append('>');
        }
        return sb.toString();
    }
    
    String getNodeTypeSymbol() {
        return function ? "#function" : "#macro";
    }
    
    boolean isShownInStackTrace() {
        return false;
    }
    
    boolean isNestedBlockRepeater() {
        // Because of recursive calls
        return true;
    }
    public boolean isFunction() {
        return function;
    }

    int getParameterCount() {
        return 1/*name*/ + paramNames.length * 2/*name=default*/ + 1/*catchAll*/ + 1/*type*/;
    }

    Object getParameterValue(int idx) {
        if (idx == 0) {
            return name;
        } else {
            final int argDescsEnd = paramNames.length * 2 + 1;
            if (idx < argDescsEnd) {
                String paramName = paramNames[(idx - 1) / 2];
                if (idx % 2 != 0) {
                    return paramName;
                } else {
                    return paramDefaults.get(paramName);
                }
            } else if (idx == argDescsEnd) {
                return catchAllParamName;
            } else if (idx == argDescsEnd + 1) {
                return new Integer(function ? TYPE_FUNCTION : TYPE_MACRO);
            } else {
                throw new IndexOutOfBoundsException();
            }
        }
    }

    ParameterRole getParameterRole(int idx) {
        if (idx == 0) {
            return ParameterRole.ASSIGNMENT_TARGET;
        } else {
            final int argDescsEnd = paramNames.length * 2 + 1;
            if (idx < argDescsEnd) {
                if (idx % 2 != 0) {
                    return ParameterRole.PARAMETER_NAME;
                } else {
                    return ParameterRole.PARAMETER_DEFAULT;
                }
            } else if (idx == argDescsEnd) {
                return ParameterRole.CATCH_ALL_PARAMETER_NAME;
            } else if (idx == argDescsEnd + 1) {
                return ParameterRole.AST_NODE_SUBTYPE;
            } else {
                throw new IndexOutOfBoundsException();
            }
        }
    }
    
    @Override
    public String toString() {
        final UnboundTemplate unboundTemplate = getUnboundTemplate();
        return "UnboundCallable("
                + "name=" + getName()
                + ", isFunction=" + isFunction()
                + ", unboundTemplate"
                + (unboundTemplate != null ? ".sourceName=" + unboundTemplate.getSourceName() : "=null")
                + ")";
    }

}
