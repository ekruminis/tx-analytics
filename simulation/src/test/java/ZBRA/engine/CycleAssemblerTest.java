package ZBRA.engine;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InOrder;
import org.mockito.Mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import org.mockito.junit.jupiter.MockitoExtension;

import ZBRA.blockchain.Transaction;

@ExtendWith(MockitoExtension.class)
class CycleAssemblerTest {

    @Mock
    SimulationEngine engine;

    @Test
    void drainsBufferedCyclesInOrderOnceEofArrives() {
        CycleAssembler assembler = new CycleAssembler(List.of(engine), 1);
        Transaction a = tx("a");
        Transaction b = tx("b");

        assembler.accept(0, 1, "ds", a);
        assembler.accept(0, 2, "ds", b);
        assembler.markPartitionEof(0);

        InOrder ordered = inOrder(engine);
        ordered.verify(engine).mineCycle("ds", 1, List.of(a));
        ordered.verify(engine).mineCycle("ds", 2, List.of(b));
        verifyNoMoreInteractions(engine);
    }

    @Test
    void withholdsACycleUntilEveryPartitionHasPassedIt() {
        CycleAssembler assembler = new CycleAssembler(List.of(engine), 2);
        Transaction a = tx("a");
        Transaction b = tx("b");
        Transaction c = tx("c");
        Transaction d = tx("d");

        assembler.accept(0, 1, "ds", a);
        assembler.accept(1, 1, "ds", b);
        verifyNoInteractions(engine);

        assembler.accept(0, 2, "ds", c);
        verifyNoInteractions(engine);

        assembler.accept(1, 2, "ds", d);
        verify(engine).mineCycle(eq("ds"), eq(1L),
                argThat(txs -> txs.size() == 2 && txs.containsAll(List.of(a, b))));

        assembler.markPartitionEof(0);
        assembler.markPartitionEof(1);
        verify(engine).mineCycle(eq("ds"), eq(2L),
                argThat(txs -> txs.size() == 2 && txs.containsAll(List.of(c, d))));
    }

    private static Transaction tx(String hash) {
        return new Transaction(hash, 10, 10, 1.0);
    }
}
