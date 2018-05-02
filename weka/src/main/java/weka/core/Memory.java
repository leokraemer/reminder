/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Memory.java
 * Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;


/**
 * A little helper class for Memory management. The memory management can be
 * disabled by using the setEnabled(boolean) method.
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 11271 $
 * @see #setEnabled(boolean)
 */
public class Memory implements RevisionHandler {

  public static final long OUT_OF_MEMORY_THRESHOLD = 52428800L;

  public static final long LOW_MEMORY_MINIMUM = 104857600L;

  public static final long MAX_SLEEP_TIME = 10L;

  /** whether memory management is enabled */
  protected boolean m_Enabled = true;

  /** whether a GUI is present */
  protected boolean m_UseGUI = false;

  /** the delay before testing for out of memory */
  protected long m_SleepTime = MAX_SLEEP_TIME;

  /**
   * initializes the memory management without GUI support
   */
  public Memory() {
    this(false);
  }

  /**
   * initializes the memory management
   * 
   * @param useGUI whether a GUI is present
   */
  public Memory(boolean useGUI) {
    m_UseGUI = useGUI;
  }

  /**
   * returns whether the memory management is enabled
   * 
   * @return true if enabled
   */
  public boolean isEnabled() {
    return m_Enabled;
  }

  /**
   * sets whether the memory management is enabled
   * 
   * @param value true if the management should be enabled
   */
  public void setEnabled(boolean value) {
    m_Enabled = value;
  }

  /**
   * whether to display a dialog in case of a problem (= TRUE) or just print on
   * stderr (= FALSE)
   * 
   * @return true if the GUI is used
   */
  public boolean getUseGUI() {
    return m_UseGUI;
  }


  /**
   * returns the amount of bytes as MB
   * 
   * @return the MB amount
   */
  public static double toMegaByte(long bytes) {
    return (bytes / (double) (1024 * 1024));
  }


  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11271 $");
  }

}
