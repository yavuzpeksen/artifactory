/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

var DisabledAddon = {
    create: function(id) {
        // create only once
        var node = dojo.byId(id);
        if (node.DisabledAddon) {
            return;
        }
        node.DisabledAddon = true;

        // add tooltip
        DojoUtils.instantiate(id + '_bubble');

        // add icon
        var icon = document.createElement('span');
        icon.className = 'addon-icon';
        node.insertBefore(icon, node.firstChild);
    },

    toogle: function(link, id, serverToken, addon) {
        // toggle show/hide
        if (!link.className.match(/checked/)) {
            dojo.cookie('addon-' + addon, serverToken, {expires: 3650});
            link.className = 'hide-link hide-link-checked';
        } else {
            dojo.cookie('addon-' + addon, null, {expires: -1});
            link.className = 'hide-link';
        }

        // sync className
        dojo.byId(id + '_hide').className = link.className;
        return false;
    }
};