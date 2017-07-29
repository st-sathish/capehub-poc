package com.capestartproject.common.util;

import static com.capestartproject.common.util.data.Monadics.mlist;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.capestartproject.common.util.data.Collections;
import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Function2;
import com.capestartproject.common.util.data.Monadics;
import com.capestartproject.common.util.data.Option;
import com.capestartproject.common.util.data.Prelude;

/** JSON builder based on json-simple. */
public final class Jsons {
  private Jsons() {
  }

  /** Check if a value is not {@link #ZERO_VAL}. */
  public static final Function<Val, Boolean> notZero = new Function<Val, Boolean>() {
    @Override public Boolean apply(Val val) {
      return !ZERO_VAL.equals(val);
    }
  };

  /** Get the value from a property. */
  public static final Function<Prop, Val> getVal = new Function<Prop, Val>() {
    @Override public Val apply(Prop prop) {
      return prop.getVal();
    }
  };

  /** {@link #toJson(org.opencastproject.util.Jsons.Obj)} as a function. */
  public static final Function<Obj, String> toJson = new Function<Obj, String>() {
    @Override public String apply(Obj obj) {
      return obj.toJson();
    }
  };

  /** JSON null. */
  public static final Val NULL = new Val() {
  };

  /** Identity for {@link Val values}. */
  public static final Val ZERO_VAL = new Val() {
  };

  /** Identity for {@link Obj objects}. */
  public static final Obj ZERO_OBJ = obj();

  /** Identity for {@link Arr arrays}. */
  public static final Arr ZERO_ARR = arr();

  public static final class Prop {
    private final String name;
    private final Val val;

    private Prop(String name, Val val) {
      this.name = name;
      this.val = val;
    }

    public String getName() {
      return name;
    }

    public Val getVal() {
      return val;
    }
  }

  // sum type
  public abstract static class Val {
  }

  private static final class SVal extends Val {
    private final Object val;

    private SVal(Object val) {
      this.val = val;
    }

    public Object getVal() {
      return val;
    }
  }

  public static final class Obj extends Val {
    private final List<Prop> props;

    private Obj(List<Prop> props) {
      this.props = props;
    }

    public List<Prop> getProps() {
      return props;
    }

    public Obj append(Obj o) {
      if (!ZERO_OBJ.equals(o))
        return new Obj(Collections.<Prop, List>concat(props, o.getProps()));
      else
        return o;
    }

    public String toJson() {
      return Jsons.toJson(this);
    }
  }

  public static final class Arr extends Val {
    private final List<Val> vals;

    public Arr(List<Val> vals) {
      this.vals = vals;
    }

    public List<Val> getVals() {
      return vals;
    }

    public Arr append(Arr a) {
      if (!ZERO_ARR.equals(a))
        return new Arr(Collections.<Val, List>concat(vals, a.getVals()));
      else
        return a;
    }

    public String toJson() {
      return Jsons.toJson(this);
    }
  }

  //

  public static String toJson(Obj obj) {
    return toJsonSimple(obj).toString();
  }

  public static String toJson(Arr arr) {
    return toJsonSimple(arr).toString();
  }

  private static JSONObject toJsonSimple(Obj obj) {
    return mlist(obj.getProps()).foldl(new JSONObject(), new Function2<JSONObject, Prop, JSONObject>() {
      @Override public JSONObject apply(JSONObject jo, Prop prop) {
        jo.put(prop.getName(), toJsonSimple(prop.getVal()));
        return jo;
      }
    });
  }

  private static JSONArray toJsonSimple(Arr arr) {
    return mlist(arr.getVals()).foldl(new JSONArray(), new Function2<JSONArray, Val, JSONArray>() {
      @Override public JSONArray apply(JSONArray ja, Val val) {
        ja.add(toJsonSimple(val));
        return ja;
      }
    });
  }

  private static Object toJsonSimple(Val val) {
    if (val instanceof SVal) {
      return ((SVal) val).getVal();
    }
    if (val instanceof Obj) {
      return toJsonSimple((Obj) val);
    }
    if (val instanceof Arr) {
      return toJsonSimple((Arr) val);
    }
    if (val.equals(NULL)) {
      return null;
    }
    return Prelude.unexhaustiveMatch();
  }

  /** Create an object. */
  public static Obj obj(Prop... ps) {
    return new Obj(mlist(ps).filter(notZero.o(getVal)).value());
  }

  /** Create an array. */
  public static Arr arr(Val... vs) {
    return new Arr(mlist(vs).filter(notZero).value());
  }

  /** Create an array. */
  public static Arr arr(List<Val> vs) {
    return new Arr(mlist(vs).filter(notZero).value());
  }

  /** Create an array. */
  public static Arr arr(Monadics.ListMonadic<Val> vs) {
    return new Arr(vs.filter(notZero).value());
  }

  public static Val v(Number v) {
    return new SVal(v);
  }

  public static Val v(String v) {
    return new SVal(v);
  }

  public static final Function<String, Val> stringVal = new Function<String, Val>() {
    @Override public Val apply(String s) {
      return v(s);
    }
  };

  public static Val v(Boolean v) {
    return new SVal(v);
  }

  public static Val v(Date v) {
    return new SVal(DateTimeSupport.toUTC(v.getTime()));
  }

  /** Create a property. */
  public static Prop p(String key, Val val) {
    return new Prop(key, val);
  }

  /** Create a property. Passing none is like setting {@link #ZERO_VAL} which erases the property. */
  public static Prop p(String key, Option<Val> val) {
    return new Prop(key, val.getOrElse(ZERO_VAL));
  }

  /** Create a property. Convenience. */
  public static Prop p(String key, Number value) {
    return new Prop(key, v(value));
  }

  /** Create a property. Convenience. */
  public static Prop p(String key, String value) {
    return new Prop(key, v(value));
  }

  /** Create a property. Convenience. */
  public static Prop p(String key, Boolean value) {
    return new Prop(key, v(value));
  }

  /** Create a property. Convenience. */
  public static Prop p(String key, Date value) {
    return new Prop(key, v(value));
  }

  /** Merge a list of objects into one (last one wins). */
  public static Obj append(Obj... os) {
    final List<Prop> props = mlist(os).foldl(new ArrayList<Prop>(), new Function2<ArrayList<Prop>, Obj, ArrayList<Prop>>() {
      @Override public ArrayList<Prop> apply(ArrayList<Prop> props, Obj obj) {
        props.addAll(obj.getProps());
        return props;
      }
    });
    return new Obj(props);
  }

  /** Append a list of arrays into one. */
  public static Arr append(Arr... as) {
    final List<Val> vals = mlist(as).foldl(new ArrayList<Val>(), new Function2<ArrayList<Val>, Arr, ArrayList<Val>>() {
      @Override public ArrayList<Val> apply(ArrayList<Val> vals, Arr arr) {
        vals.addAll(arr.getVals());
        return vals;
      }
    });
    return new Arr(vals);
  }
}
