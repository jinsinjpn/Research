package demandGeneratorGunnma;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author parady_UT
 * person trip data (Japan) parser for basic demand generation
*  author: parady_UT; adapted from Zurich travel census case
 */

public class PTParser {
	private String separator = ",";
	private Charset charset = Charset.forName("UTF-8");

	public List<PTEntry> readFile(String inFile)
	{
		List<PTEntry> entries = new ArrayList<PTEntry>();

		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;

    	try
    	{
    		fis = new FileInputStream(inFile);
    		isr = new InputStreamReader(fis, charset);
			br = new BufferedReader(isr);

			// skip first Line
			br.readLine();

			String line;
			while((line = br.readLine()) != null)
			{
				PTEntry censusEntry = new PTEntry();

				String[] cols = line.split(separator);

				censusEntry.id_person = parseInteger(cols[0]);
				censusEntry.id_person2 = parseInteger(cols[1]);
				censusEntry.id_person3 = parseInteger(cols[2]);
				censusEntry.id_person4 = parseInteger(cols[3]);
				censusEntry.tripnum = parseInteger(cols[4]);
				censusEntry.starttime = parseInteger(cols[5]);
				censusEntry.h_x = parseDouble(cols[6]);
				censusEntry.h_y = parseDouble(cols[7]);
				censusEntry.s_x = parseDouble(cols[8]);
				censusEntry.s_y = parseDouble(cols[9]);
				censusEntry.d_x = parseDouble(cols[10]);
				censusEntry.d_y = parseDouble(cols[11]);
				censusEntry.bike = parseInteger(cols[12]);
				censusEntry.age = parseInteger(cols[13]);
				censusEntry.gender = parseInteger(cols[14]);
				censusEntry.license = parseInteger(cols[15]);
				censusEntry.caravailability = parseInteger(cols[16]);
				censusEntry.car = parseInteger(cols[17]);
				censusEntry.day = parseInteger(cols[18]);
				censusEntry.tripmode = parseInteger(cols[19]);
				censusEntry.trippurpose = parseInteger(cols[20]);
				censusEntry.tripdistance = parseDouble(cols[21]);
				censusEntry.tripduration = parseInteger(cols[22]);
				censusEntry.id_tour = parseInteger(cols[23]);
				censusEntry.strict = parseInteger(cols[24]);
				censusEntry.arrivetime = parseInteger(cols[25]);

				entries.add(censusEntry);
			}

			br.close();
			isr.close();
			fis.close();
    	}
    	catch (FileNotFoundException e)
    	{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return entries;
	}

	private int parseInteger(String string)
	{
		if (string == null) return 0;
		else if (string.trim().isEmpty()) return 0;
		else return Integer.valueOf(string);
	}

	private double parseDouble(String string)
	{
		if (string == null) return 0.0;
		else if (string.trim().isEmpty()) return 0.0;
		else return Double.valueOf(string);
	}
}
