import java.util.*;

import soot.*;
import soot.jimple.*;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;





public class AddTransform extends SceneTransformer {
    static CallGraph cg1;

    int freshId;
    private boolean dfs(SootMethod callee,List<SootMethod> l){
        if(callee.isJavaLibraryMethod()) return true;
        //System.out.println("\n&&&&&&&&&&&&&&&&&&&&&&&&&callee from isinlinable "+callee);
        l.add(callee);
        for(Unit unit:callee.getActiveBody().getUnits()){
            Stmt st = (Stmt) unit;

            if (st.containsInvokeExpr()) {
                InvokeExpr ie = st.getInvokeExpr();
                SootMethod nextmtd = ie.getMethod();
                if(l.contains(nextmtd)) return false;
                if(!dfs(nextmtd,l)) return false;
            }
        }
        l.remove(callee);
        return true;
    }

    private boolean isInlinable(Unit u){

        Stmt stmt = (Stmt) u;
        Iterator<Edge> targets = cg1.edgesOutOf(stmt);

        int count = 0;
        while  (targets.hasNext()) {
            targets.next();
            count++;
        }
        
        if(count>1) return false;

        targets = cg1.edgesOutOf(stmt);
        SootMethod callee = targets.next().tgt();
        if(callee==null) return false; 
        System.out.println("********hello_world************");
        
        if(callee.isAbstract() || callee.isNative()) return false;

        //if(callee.equals(caller)) return false;

        if(callee.hasActiveBody() && callee.getActiveBody().getUnits().size()>30) return false;

        if(callee.isJavaLibraryMethod()) return false;
        
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
        List<SootMethod> l = new ArrayList<>();
        if(!dfs(callee,l)) return false;

        return true;
    }
    
    private void createcfg(){
        // 1. Release old graph
        Scene.v().releaseCallGraph();
        Scene.v().releasePointsToAnalysis();

        // 2. Ensure classes are loaded
        Scene.v().loadNecessaryClasses();

        // 3. Ensure entry points exist (VERY IMPORTANT)
        SootMethod mainMethod = Scene.v().getMainMethod();
        Scene.v().setEntryPoints(Collections.singletonList(mainMethod));

        // 4. Rebuild SPARK
        HashMap<String, String> opts = new HashMap<>();
        opts.put("enabled", "true");
        opts.put("on-fly-cg", "true");

        
        SparkTransformer.v().transform("", opts);


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

        freshId=0;
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
        
        for(int i=0;i<2;i++){
        // Iterate over all units (instructions) in the method body
        
        PatchingChain<Unit> units = entryMethod.retrieveActiveBody().getUnits();

        // Iterate over instructions and replace iadd with imul
        Iterator<Unit> unitIt = units.snapshotIterator();

        System.out.println("Going for next iter");
        while (unitIt.hasNext()) {
            Unit unit = unitIt.next();
            //System.out.println(unit);

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

            if(unit instanceof InvokeStmt || (unit instanceof AssignStmt && ((AssignStmt)unit).getRightOp() instanceof InvokeExpr)){

                System.out.println("check for"+unit);
                System.out.println();

                if(!isInlinable(unit)) continue;
                Stmt st = (Stmt) unit;
                InvokeExpr ie = st.getInvokeExpr();
                SootMethod callee = ie.getMethod();
                // Body calleeBody = callee.retrieveActiveBody();
                // Body cloned = (Body) calleeBody.clone();
                // cloned.getLocals()

                System.out.println("calling ineline for "+unit);
                inlinecallsite(entryMethod.retrieveActiveBody(),units,st,ie,callee);
            }
            // else if(unit instanceof AssignStmt){
            //     AssignStmt as= (AssignStmt)unit;
            //     Value right = as.getRightOp();
            // }
        }
        createcfg();
        cg1 = Scene.v().getCallGraph();

      }
    }




/*     private void inlinecallsite(Body callerBody, PatchingChain<Unit> units, Stmt callSite, InvokeExpr callExpr, SootMethod callee){
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
                    else if(callExpr instanceof SpecialInvokeExpr)
                    valueMap.put((Local) id.getLeftOp(), ((SpecialInvokeExpr)callExpr).getBase());  // l0 = @this;

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




        System.out.println("Printing callee units\n");
        // Insert cloned callee statements before "afterInline".
        for (Unit cu : calleeBody.getUnits()) {
            System.out.println(cu);
            if (cu instanceof IdentityStmt) {
                continue; // parameters , this already mapped
            }

            Unit cloned = (Unit) cu.clone();
            
            
            // Replace locals inside the cloned statement.
            replaceValues(cloned, valueMap);
            System.out.println("cloned : "+cloned);

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
        units.remove(callSite);

    }

*/


        private void inlinecallsite(Body callerBody, PatchingChain<Unit> units, Stmt callSite, InvokeExpr callExpr, SootMethod callee){
        Body calleeBody = callee.retrieveActiveBody();

        calleeBody.validate();
        System.out.println(callee.getSignature());
        // Create a label after the inlined code.
        NopStmt afterInline = Jimple.v().newNopStmt();
        units.insertBefore(afterInline, callSite);

        NopStmt nopforgoto = Jimple.v().newNopStmt();
        units.insertAfter(nopforgoto, callSite);


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
                    else if(callExpr instanceof SpecialInvokeExpr)
                    valueMap.put((Local) id.getLeftOp(), ((SpecialInvokeExpr)callExpr).getBase());  // l0 = @this;

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


        Map<Unit, Unit> unitMap = new HashMap<>();
        List<Unit> clonedUnits = new ArrayList<>();

        for (Unit oldU : calleeBody.getUnits()) {

            // System.out.println("QWERTY      ->     "+oldU);
            // for(UnitBox ubox : oldU.getUnitBoxes())
            //     System.out.print(ubox.getUnit()+" ");
            // System.out.println();

            if (oldU instanceof IdentityStmt) continue;
            
            Unit newU = (Unit) oldU.clone();
            replaceValues(newU, valueMap); // locals mapping
            unitMap.put(oldU, newU);
            clonedUnits.add(newU);
        }


        System.out.println("##############Printing Map#############\n");
        for(Map.Entry<Unit,Unit> entry:unitMap.entrySet()){
            System.out.println(entry.getKey()+"  :  "+entry.getValue());
        }

        for (Unit newU : clonedUnits) {

            if (newU instanceof GotoStmt) {
                GotoStmt g = (GotoStmt) newU;

                Unit oldTarget = g.getTarget();
                //System.out.println("oldtarget "+oldTarget);
                Unit newTarget = unitMap.get(oldTarget);

                g.setTarget(newTarget);
                System.out.println("efeg    .... "+ g.getTarget());
                if (g.getTarget() instanceof ReturnVoidStmt || g.getTarget() instanceof ReturnStmt) {
                    //units.insertBefore(Jimple.v().newGotoStmt(nopforgoto), afterInline);
                    g.setTarget(nopforgoto);
                continue;
                }
            }

            else if (newU instanceof IfStmt) {
                IfStmt ifs = (IfStmt) newU;

                Unit oldTarget = ifs.getTarget();
                Unit newTarget = unitMap.get(oldTarget);

                ifs.setTarget(newTarget);

                if (ifs.getTarget() instanceof ReturnVoidStmt || ifs.getTarget() instanceof ReturnStmt) {
                    //units.insertBefore(Jimple.v().newGotoStmt(nopforgoto), afterInline);
                    ifs.setTarget(nopforgoto);
                continue;
                }
            }
        }

        System.out.println("Printing callee units\n");
        // Insert cloned callee statements before "afterInline".

        for (Unit u : clonedUnits) {
        
            System.out.println(u);
            if (u instanceof ReturnStmt) {
                ReturnStmt rs = (ReturnStmt) u;

                Value retVal = rs.getOp();

                if (callSite instanceof AssignStmt) {
                    Value lhs = ((AssignStmt) callSite).getLeftOp();
                    units.insertBefore(Jimple.v().newAssignStmt(lhs, retVal), afterInline);
                }

                units.insertBefore(Jimple.v().newGotoStmt(nopforgoto), afterInline);
                continue;
            }

            if (u instanceof ReturnVoidStmt) {
                units.insertBefore(Jimple.v().newGotoStmt(nopforgoto), afterInline);
                continue;
            }
            units.insertBefore(u, afterInline);
        }
        for (Unit u : callerBody.getUnits()) {
    for (UnitBox ub : u.getUnitBoxes()) {
        if (!callerBody.getUnits().contains(ub.getUnit())) {
            throw new RuntimeException("Dangling target: " + u + " -> " + ub.getUnit());
        }
    }
}

        // Remove the original call site.
        units.remove(callSite);

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
