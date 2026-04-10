# CS6004_PA4
Lets Beat the Interpreter

# CS6004: Code Optimization for OO Languages  
## Programming Assignment 4: Beat the Interpreter

**Release Date:** March 24, 2026  
**Due Date:** April 18, 2026  

---

## 📌 Overview
This project implements object-oriented optimizations using **Soot** through static analysis and bytecode transformation. The goal is to improve execution performance of Java programs running in interpreter mode.

---

## 🎯 Objectives
- Identify optimization opportunities using static analysis  
- Apply transformations on Java bytecode via Soot  
- Demonstrate correctness and performance improvements  

---

## ⚙️ Optimization Implemented
*(Edit this section based on your work)*

Example:
- Monomorphization of virtual calls using call graph analysis  
- Method inlining for frequently invoked small methods  
- Null-check elimination  

---

## 🧠 Analysis
*(Edit this section)*

- Analysis Type: Flow-sensitive / Flow-insensitive / Context-sensitive  
- Techniques Used: CHA / VTA / Pointer Analysis  
- Assumptions: Clearly state any assumptions for soundness  

---

## 🔄 Transformation
*(Edit this section)*

- Description of transformations applied  
- Example before/after snippets  
- Challenges and how they were handled  

---

## 📊 Performance Evaluation

### Metrics Used
- Wall-clock execution time  
- Reduction in virtual call sites (or other custom metrics)

### Setup
- Executed using:
  ```bash
  java -Xint