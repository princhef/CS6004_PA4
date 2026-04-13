import java.util.*;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;





public class AddTransform extends SceneTransformer {
    static CallGraph cg1;


    private boolean isInlinable(Unit u){

        Stmt stmt = (Stmt) u;
        Iterator<Edge> targets = cg1.edgesOutOf(stmt);

        int count = 0;
        while (targets.hasNext()) {
            targets.next();
            count++;
        }
        
        if(count>1) return false;

        targets = cg1.edgesOutOf(stmt);
        SootMethod callee = targets.next().tgt();
        
        if(callee.isAbstract() || callee.isNative()) return false;

        //if(callee.equals(caller)) return false;

        if(callee.hasActiveBody() && callee.getActiveBody().getUnits().size()>30) return false;

        
        //handle recursive
        for(Unit unit:callee.getActiveBody().getUnits()){
            Stmt st = (Stmt) unit;

            if (st.containsInvokeExpr()) {
                InvokeExpr ie = st.getInvokeExpr();
                SootMethod nextmtd = ie.getMethod();
                if(nextmtd.equals(callee)) return false;
            }
        }

        //implement cyclic recursive   /*##################################### To be implemented ############################3 */

        return true;
    }
    
    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        // Store the call graph once as a static field...
        cg1 = Scene.v().getCallGraph();

        // This code lets us get the main method, our testcases will only have one start
        // point that is the main method
        // in the Test class...
        var entrypoints = Scene.v().getEntryPoints();
        //assert (entrypoints.size() == 1);
        SootMethod entryMethod = entrypoints.get(0);

        //handleMainMethod(entryMethod);

       
  //  }
//}



//public class AddTransform extends BodyTransformer {

    //CallGraph cg1;

    //@Override
    //protected void internalTransform(Body body, String phaseName, Map<String, String> options) {

        //if(body.getMethod().isConstructor()) return;
        //if(body.getMethod().isJavaLibraryMethod()) return;

        //System.out.println(body.getMethod().getSignature());
        //cg1 = Scene.v().getCallGraph();
        
        // Iterate over all units (instructions) in the method body
        PatchingChain<Unit> units = entryMethod.retrieveActiveBody().getUnits();

        // Iterate over instructions and replace iadd with imul
        Iterator<Unit> unitIt = units.snapshotIterator();

        while (unitIt.hasNext()) {
            Unit unit = unitIt.next();
            System.out.println(unit);

         /*    if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;

                // Check if it's an iadd operation
                if (stmt instanceof AssignStmt) {
                    AssignStmt assignStmt = (AssignStmt) stmt;
                    Value rightOp = assignStmt.getRightOp();

                    if (rightOp instanceof AddExpr && rightOp.getType() instanceof IntType) {
                        AddExpr addExpr = (AddExpr) rightOp;

                        if (addExpr.getOp1().getType() instanceof IntType && addExpr.getOp2().getType() instanceof IntType) {
                            // Create a new multiplication expression
                            MulExpr mulExpr = Jimple.v().newMulExpr(addExpr.getOp1(), addExpr.getOp2());
                            // Replace iadd with imul
                            assignStmt.setRightOp(mulExpr);
                        }
                    }
                }
            }
            */

            if(unit instanceof InvokeStmt){

                if(!isInlinable(unit)) continue;
                Stmt st = (Stmt) unit;
                InvokeExpr ie = st.getInvokeExpr();
                SootMethod callee = ie.getMethod();
                // Body calleeBody = callee.retrieveActiveBody();
                // Body cloned = (Body) calleeBody.clone();
                // cloned.getLocals()

                inlinecallsite(entryMethod.retrieveActiveBody(),units,st,ie,callee);
            }
            else if(unit instanceof AssignStmt){
                AssignStmt as= (AssignStmt)unit;
                Value right = as.getRightOp();
            }
        }
    }

    private void inlinecallsite(Body callerBody, PatchingChain<Unit> units, Stmt callSite, InvokeExpr callExpr, SootMethod callee){
        Body calleeBody = callee.retrieveActiveBody();

        
        // Create a label after the inlined code.
        NopStmt afterInline = Jimple.v().newNopStmt();
        units.insertBefore(afterInline, callSite);

        // Map callee locals to caller locals / actual arguments.
        Map<Local, Value> valueMap = new HashMap<>();

        // If the callee uses "this", map its @this local to the receiver object.
        // We detect the local from the identity statement: l0 := @this
        for (Unit cu : calleeBody.getUnits()) {
            
            if (cu instanceof IdentityStmt) {
                
                IdentityStmt id = (IdentityStmt) cu;
                
                if (id.getRightOp() instanceof ThisRef) {
                    if(callExpr instanceof VirtualInvokeExpr)
                    valueMap.put((Local) id.getLeftOp(), ((VirtualInvokeExpr)callExpr).getBase());  // l0 = @this;
                }

                else if (id.getRightOp() instanceof ParameterRef) {
                    ParameterRef pr = (ParameterRef) id.getRightOp();  // r0= @param0;
                    Value actualArg = callExpr.getArg(pr.getIndex());
                    valueMap.put((Local) id.getLeftOp(), actualArg);
                }
            }
        }

        // Fresh locals for every callee local that is used as a normal local.

        List<Local> toAdd = new ArrayList<>();


        int freshId=0;
        for (Local oldLocal : calleeBody.getLocals()) {
            if (!valueMap.containsKey(oldLocal)) {
                Local fresh = Jimple.v().newLocal(
                        "$inlined_" +callSite.getJavaSourceStartLineNumber()+"_"+callee.getName()+"_"+(freshId++),
                        oldLocal.getType());
                toAdd.add(fresh);
                valueMap.put(oldLocal, fresh);
            }
        }
        callerBody.getLocals().addAll(toAdd);



        // Insert cloned callee statements before "afterInline".
        for (Unit cu : calleeBody.getUnits()) {
            if (cu instanceof IdentityStmt) {
                continue; // parameters , this already mapped
            }

            Unit cloned = (Unit) cu.clone();

            // Replace locals inside the cloned statement.
            replaceValues(cloned, valueMap);

            // Handle returns.
            if (cloned instanceof ReturnVoidStmt) {
                units.insertBefore(Jimple.v().newGotoStmt(callSite), afterInline);
                continue;
            }

            if (cloned instanceof ReturnStmt) {
                ReturnStmt rs = (ReturnStmt) cloned;
                Value retVal = rs.getOp();

                // If original call was "x = obj.m(...)", assign the return value to x.
                if (callSite instanceof AssignStmt) {
                    Value lhs = ((AssignStmt) callSite).getLeftOp();
                    units.insertBefore(Jimple.v().newAssignStmt(lhs, retVal), afterInline);
                }

                units.insertBefore(Jimple.v().newGotoStmt(callSite), afterInline);
                continue;
            }

            units.insertBefore(cloned, afterInline);
        }

        // Remove the original call site.
        //units.remove(callSite);

    }


    private void replaceValues(Unit u, Map<Local, Value> valueMap) {
        for (ValueBox vb : u.getUseBoxes()) {
            Value v = vb.getValue();
            if (v instanceof Local && valueMap.containsKey(v)) {
                vb.setValue(valueMap.get(v));
            }
        }

        for (ValueBox vb : u.getDefBoxes()) {
            Value v = vb.getValue();
            if (v instanceof Local && valueMap.containsKey(v)) {
                vb.setValue(valueMap.get(v));
            }
        }
    }

}
