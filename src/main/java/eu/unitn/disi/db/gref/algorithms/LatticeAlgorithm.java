/*
 * The MIT License
 *
 * Copyright 2014 Davide Mottin <mottin@disi.unitn.eu>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package eu.unitn.disi.db.gref.algorithms;

import de.parmol.graph.Graph;
import eu.unitn.disi.db.command.algorithmic.AlgorithmInput;
import eu.unitn.disi.db.gref.lattice.ReformulationLattice;

/**
 * A lattice algorithm 
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public abstract class LatticeAlgorithm extends ReformulationAlgorithm {
    @AlgorithmInput(
        description = "Set of graphs used as database",
        mandatory = true
    )
    protected Graph[] gdb;

    public void setDb(Graph[] gdb) {
        this.gdb = gdb;
    }

    @AlgorithmInput(
            description = "Lattice with the initial mapping", 
            mandatory = true
    )
    protected ReformulationLattice lattice;

    public void setLattice(ReformulationLattice lattice) {
        this.lattice = lattice;
    }
}