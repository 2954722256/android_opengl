package com.nvidia.developer.opengl.ui;

/**
 * A templated subclass of NvTweakVarUI specifically for handling one entry of
 * an NvTweakEnum list/array passed into the tweakbar -- i.e., one button in a
 * radio button group, or one menu item in a popup menu. Each NvTweakVarUI
 * element is responsible for setting its stored enum value on the linked
 * NvTweakVar when the UI element is selected.
 * 
 * @author Nvidia 2014-9-13 21:11
 * 
 */
public class NvTweakEnumUIi extends NvTweakVarUIi {

	/** A reference to the enum var that we represent one entry from. */
	protected NvTweakEnumVari m_tevar;
	/** The array index of the enum entry that our UI item represents. */
	protected int m_teindex;
	/**
	 * A cached value passed in via an NvTweakEnum entry.
	 * <p>
	 * Will be used to set our linked NvTweakVar to a specific value during an
	 * appropriate handleReaction.
	 */
	protected int m_enumval;

	/**
	 * Normal constructor. Takes the referenced NvTweakEnumVar, the enumerant
	 * index we represent, the individual UI element that acts as our proxy for
	 * user interaction in a radio group or popup menu, and an optional override
	 * of the action code for the variable and UI.
	 */
	public NvTweakEnumUIi(NvTweakEnumVari tever, int index, NvUIElement el,
			int actionCode /* =0 */) {
		super(tever, el, actionCode);
		m_tevar = tever;
		m_teindex = index;
		m_enumval = tever.get(index); // cache the value at the given index for
										// easier access later.
	}

	/**
	 * We override handleEvent so that if there is a reaction by the proxied UI
	 * widget, we can replace the value inside the NvUIReaction with our cached
	 * value, and allow the rest of the reaction process to occur as normal.
	 */
	@Override
	public int handleEvent(NvGestureEvent ev, long timeUST,
			NvUIElement hasInteract) {
		int r =  super.handleEvent(ev, timeUST, hasInteract);
		if ((r&nvuiEventHadReaction) != 0)
	    {
	        NvUIReaction react = getReactionEdit(false); // false to not clear it!!!
	        react.ival = m_enumval;
	    }
	    return r;
	}

	/**
	 * We override handleReaction so that when there is an NvUIReaction passing
	 * through the system containing a real value, we can set our state to be 0
	 * or 1 (active or pressed) based on whether our cached value matches the
	 * value of the NvTweakVar we're bound to. This is so radio buttons and menu
	 * items update their visual state to match outside changes to the
	 * NvTweakVar's value (such as from key input).
	 */
	@Override
	public int handleReaction(NvUIReaction react) {
		if (react.code != 0 && (react.code!=m_tvar.getActionCode()))
	        return nvuiEventNotHandled; // not a message for us.

	    int r = nvuiEventNotHandled;
	    
	    NvUIReaction change = getReactionEdit(false); // false to not clear it!!!        
	    // if it's a system|forced update, we assume value changed but button/proxy state needs update.
	    if ((change.flags & NvReactFlag.FORCE_UPDATE) != 0)
	    {
	        if (m_tvar.get() != m_enumval) 
	        { // if tweakvar value is not our enum, set state to 0/inactive
	            change.state = 0;
	        }
	        else 
	        { // tweakvar value matches our enum, so set state to 1/active
	            change.state = 1;
	            // in case someone wants it, fill in our value since we matched the tweakvar value..
	            change.ival = m_enumval;
	        }
	    }
	    else if ((change.flags & NvReactFlag.CLEAR_STATE) != 0)
	    {
	        change.state = 0; // clear drawstate of all els who match our actioncode.
	    }
	    // else we leave reaction alone.

	    // if we sent ourselves the reaction, we need to let NvTweakVarUI handle...
	    if (change.uid==getUID())
	    {
	        super.handleReaction(change);
	    }
	    else
	    {
	        // if we didn't handle self-reactions, then here we need to skip over
	        // what NvTweakVarUI would do, go right up to proxybase instead.
	        // !!> double INHERITED <!!
//	        INHERITED::INHERITED::HandleReaction(change);
	    	super.superHandleReaction(react);
	    }
	    return r;
	}

	/**
	 * We override handleFocusEvent so that if there is a reaction by the
	 * proxied UI widget, we can replace the value inside the NvUIReaction with
	 * our cached value, and allow the rest of the reaction process to occur as
	 * normal.
	 */
	@Override
	public int handleFocusEvent(int evt) {
		int r = super.handleFocusEvent(evt);
		if ((r&nvuiEventHadReaction) != 0)
	    {
	        NvUIReaction react = getReactionEdit(false); // false to not clear it!!!
	        react.fval = m_enumval;
	    }
	    return r;
	}
}
