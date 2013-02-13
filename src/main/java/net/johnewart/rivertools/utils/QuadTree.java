package net.johnewart.rivertools.utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: jewart
 * Date: 1/19/13
 * Time: 8:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class QuadTree<Value>  {
    private Node<Value> root;

    public QuadTree(Point2D topLeft, Point2D bottomRight)
    {
        root = new Node(new Boundary(topLeft, bottomRight));
    }

    public boolean insert(Point2D point, Value value) {
        return root.insert(point, value);
    }

    public Value findNearestTo(Point2D p) {
        return findNearestTo(root, p);
    }

    private Value findNearestTo(Node h, Point2D p) {

        if(h.children() != null)
        {
            Value found = null;

            for(Node n : h.children())
            {
                if(n.contains(p))
                {
                    found = findNearestTo(n, p);
                }
            }

            // Back up a step, look in the four quadrants we're composed of for something close
            // if we didn't find it in the most localized quadrant
            if(found == null)
            {
                // Create a temporary container as a clone of ourselves
                Node tempNode = new Node(h.boundary);

                // Pull in our children's points
                for(Node n : h.children())
                {
                    tempNode.points.putAll(n.points);
                }

                // Search the temporary node
                found = (Value)tempNode.nearestTo(p);

            }

            return found;

        } else {
            // Look in here h contains p but has no children
            return (Value)h.nearestTo(p);
        }

    }

}


 // helper node data type
 class Node<Value> {
    private int nodeCapacity = 500;

    Boundary boundary;
    Node NW, NE, SE, SW;
    HashMap<Point2D, Value> points;

    Node(Boundary boundary) {
        this.boundary = boundary;
        this.points = new HashMap<>();
    }

    public Node[] children() {
        if(NW == null)
        {
            return null;
        } else {
            Node[] childNodes = { NE, NW, SE, SW };
            return childNodes;
        }
    }

    public boolean contains(Point2D p)
    {
        return this.boundary.contains(p);
    }

    public Value nearestTo(Point2D p)
    {
        if(points.keySet().size() == 0)
        {
            // Nothing in here, can't find nearest here.
            return null;
        } else {
            // Assume first point closest and do some math
            Iterator<Point2D> it = points.keySet().iterator();
            Point2D closestPoint = it.next();

            while(it.hasNext())
            {
                Point2D otherPoint = it.next();
                if(otherPoint.distance(p) < closestPoint.distance(p))
                {
                    closestPoint = otherPoint;
                }
            }

            return (Value)points.get(closestPoint);
        }
    }

    public void subdivide()
    {
        Boundary[] quadrants = boundary.subdivide();
        this.NW = new Node(quadrants[0]);
        this.NE = new Node(quadrants[1]);
        this.SE = new Node(quadrants[2]);
        this.SW = new Node(quadrants[3]);

        // Redistribute points into their quadrants
        for(Node n : children())
        {
            for(Point2D point : points.keySet())
            {
                if(n.contains(point))
                {
                    n.points.put(point, points.get(point));
                }
            }
        }
        points.clear();
    }

    public boolean insert(Point2D point, Value value)
    {
        if (!boundary.contains(point))
        {
            return false; // Can't insert this in here.
        }

        if(points.size() < nodeCapacity)
        {
            this.points.put(point, value);
            return true;
        }


        // If this quadrant is over capacity, split up and insert into another node
        if(NW == null)
        {
            //System.err.println("Splitting");
            subdivide();
        }

        if(NW.insert(point, value)) return true;
        if(NE.insert(point, value)) return true;
        if(SE.insert(point, value)) return true;
        if(SW.insert(point, value)) return true;

        // otherwise return false
        return false;
    }
}


class Boundary {
    private Point2D topLeft, bottomRight;

    Boundary(Point2D topLeft, Point2D bottomRight)
    {
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
    }

    public boolean contains(Point2D point)
    {
        return this.topLeft.getX() < point.getX() &&
               this.topLeft.getY() > point.getY() &&
               this.bottomRight.getX() > point.getX() &&
               this.bottomRight.getY() < point.getY();
    }

    /**
     *
     * @return array of boundaries in the order: NW, NE, SE, SE
     */
    public Boundary[] subdivide()
    {
        Boundary[] quadrants = new Boundary[4];
        Double centerX = Math.abs(this.topLeft.getX() - this.bottomRight.getX()) / 2.0;
        Double centerY = Math.abs(this.topLeft.getY() - this.bottomRight.getY()) / 2.0;
        Point2D center = new Point2D.Double(centerX, centerY);

        // NW
        quadrants[0] = new Boundary(topLeft, center);
        // NE
        quadrants[1] = new Boundary(new Point2D.Double(centerX, topLeft.getY()), new Point2D.Double(bottomRight.getX(), centerY));
        // SE
        quadrants[2] = new Boundary(center, bottomRight);
        // SW
        quadrants[3] = new Boundary(new Point2D.Double(topLeft.getX(), centerY), new Point2D.Double(centerX, bottomRight.getY()));

        return quadrants;
    }
}
