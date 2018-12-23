package io.rappt.model;


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.antlr.v4.runtime.ParserRuleContext;
import io.rappt.AMLParser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// Tables moved out of AppModel tree to ensure that Gson doesn't try to add fields to the output!!!
public class LookupTables {


    static public final Map<Class<? extends ParserRuleContext>, Class<? extends PIM>> declarationLookup = Collections.unmodifiableMap(
            new HashMap<Class<? extends ParserRuleContext>, Class<? extends PIM>>() {{
                put(AMLParser.NavigationContext.class, Feature.class);
                put(AMLParser.ScreenContext.class, Screen.class);
                put(AMLParser.LabelContext.class, View.Label.class);
                put(AMLParser.ImageContext.class, View.Image.class);
                put(AMLParser.TextInputContext.class, View.TextInput.class);
                put(AMLParser.ButtonContext.class, View.Button.class);
                put(AMLParser.UiBlockContext.class, View.class);
                put(AMLParser.TabContext.class, Navigation.Tab.class);
                put(AMLParser.MenuContext.class, Feature.class);
                put(AMLParser.ResourceContext.class, Resource.class);
                put(AMLParser.ActionContext.class, View.Action.class);
                put(AMLParser.MarkerContext.class, MarkerPim.class);
                put(AMLParser.ShowToastContext.class, Instruction.ShowToast.class);
                put(AMLParser.ListContext.class, ListPim.class);
                put(AMLParser.MapContext.class, View.Map.class);
                put(AMLParser.ApiContext.class, Api.class);
                put(AMLParser.DataContext.class, Data.class);
                put(AMLParser.WebContext.class, View.Web.class);
                put(AMLParser.ToContext.class, Parameter.class);
                put(AMLParser.NotificationContext.class, Instruction.Notification.class);
                put(AMLParser.ResourcePropertiesContext.class, Preference.class); // TODO: Remove the Properties
                put(AMLParser.StyleContext.class, Style.class);
                put(AMLParser.SourceContext.class, Source.class);
                put(AMLParser.ScreenParamsContext.class, PimString.class);
            }});


    static public final Multimap<Class<? extends ParserRuleContext>, Class<? extends PIM>> antlrClassToReferencedPimClass;

    static {
        antlrClassToReferencedPimClass = ArrayListMultimap.create();
        antlrClassToReferencedPimClass.put(AMLParser.AppPropertiesContext.class, Screen.class);
        antlrClassToReferencedPimClass.put(AMLParser.ToContext.class, Screen.class);
        antlrClassToReferencedPimClass.put(AMLParser.CallContext.class, Api.class);
        antlrClassToReferencedPimClass.put(AMLParser.CallContext.class, Resource.class);
        antlrClassToReferencedPimClass.put(AMLParser.IdArrayContext.class, Feature.class);
        antlrClassToReferencedPimClass.put(AMLParser.NotificationBlockContext.class, Screen.class);

        // Need two for each marker reference
        antlrClassToReferencedPimClass.put(AMLParser.PolylineContext.class, MarkerPim.class);
        antlrClassToReferencedPimClass.put(AMLParser.PolylineContext.class, MarkerPim.class);

        antlrClassToReferencedPimClass.put(AMLParser.GetPreferenceContext.class, Preference.class);
        antlrClassToReferencedPimClass.put(AMLParser.GetPreferenceContext.class, View.TextInput.class);
        antlrClassToReferencedPimClass.put(AMLParser.RemovePreferenceContext.class, Preference.class);
        antlrClassToReferencedPimClass.put(AMLParser.LabelContext.class, Parameter.class);
        antlrClassToReferencedPimClass.put(AMLParser.StyleReferenceContext.class, Style.class);
        antlrClassToReferencedPimClass.put(AMLParser.BehaviourPropertiesContext.class, View.Button.class);

        antlrClassToReferencedPimClass.put(AMLParser.SourcePropertiesContext.class, Api.class);
        antlrClassToReferencedPimClass.put(AMLParser.SourcePropertiesContext.class, Resource.class);

        antlrClassToReferencedPimClass.put(AMLParser.ToContext.class, Screen.class);
        antlrClassToReferencedPimClass.put(AMLParser.ToContext.class, PimString.class);

        antlrClassToReferencedPimClass.put(AMLParser.SourceExtrasContext.class, PimString.class);
        antlrClassToReferencedPimClass.put(AMLParser.SourceExtrasContext.class, UIBase.class);
        antlrClassToReferencedPimClass.put(AMLParser.LabelPropertiesContext.class, Source.class);

    }
}
