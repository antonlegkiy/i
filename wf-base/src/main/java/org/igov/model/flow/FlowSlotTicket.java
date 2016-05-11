package org.igov.model.flow;

import org.hibernate.annotations.Type;
import org.igov.model.core.AbstractEntity;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Ticked assigned to one or more FlowSlot's.
 * <p/>
 * User: goodg_000
 * Date: 14.06.2015
 * Time: 15:30
 */
@javax.persistence.Entity
public class FlowSlotTicket extends AbstractEntity {

    @ManyToMany(fetch = FetchType.EAGER, targetEntity=FlowSlot.class)
    @JoinTable(name = "FlowSlotTicket_FlowSlot",
            joinColumns = @JoinColumn(name = "nID_FlowSlotTicket"),
            inverseJoinColumns = @JoinColumn(name = "nID_FlowSlot"))
    private List<FlowSlot> aFlowSlot = new ArrayList<>();

    /**
     * Many-to-one soft reference to subject, which is stored in central server.
     */
    @Column
    private Long nID_Subject;

    @Column
    private Long nID_Task_Activiti;

    @Column
    @Type(type = DATETIME_TYPE)
    private DateTime sDateStart;

    @Column
    @Type(type = DATETIME_TYPE)
    private DateTime sDateFinish;

    @Column
    @Type(type = DATETIME_TYPE)
    private DateTime sDateEdit;

    public List<FlowSlot> getaFlowSlot() {
        return aFlowSlot;
    }

    public void setaFlowSlot(List<FlowSlot> aFlowSlot) {
        this.aFlowSlot = aFlowSlot;
    }

    public Long getnID_Subject() {
        return nID_Subject;
    }

    public void setnID_Subject(Long nID_Subject) {
        this.nID_Subject = nID_Subject;
    }

    public Long getnID_Task_Activiti() {
        return nID_Task_Activiti;
    }

    public void setnID_Task_Activiti(Long nID_Task_Activiti) {
        this.nID_Task_Activiti = nID_Task_Activiti;
    }

    public DateTime getsDateStart() {
        return sDateStart;
    }

    public void setsDateStart(DateTime sDateStart) {
        this.sDateStart = sDateStart;
    }

    public DateTime getsDateFinish() {
        return sDateFinish;
    }

    public void setsDateFinish(DateTime sDateFinish) {
        this.sDateFinish = sDateFinish;
    }

    public DateTime getsDateEdit() {
        return sDateEdit;
    }

    public void setsDateEdit(DateTime sDateEdit) {
        this.sDateEdit = sDateEdit;
    }
}
